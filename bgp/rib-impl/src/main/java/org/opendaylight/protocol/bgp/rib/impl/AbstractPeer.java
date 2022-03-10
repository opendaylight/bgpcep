/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteOperations;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.protocol.bgp.mode.impl.BGPRouteEntryExportParametersImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.AbstractAdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteKeyIdentifier;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPeer extends BGPPeerStateImpl implements BGPRouteEntryImportParameters, Peer,
        PeerTransactionChain, DOMTransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeer.class);

    final RTCClientRouteCache rtCache = new RTCClientRouteCache();
    final RIB rib;

    private final ClusterIdentifier clusterId;
    private final PeerRole peerRole;
    private final AsNumber localAs;
    private final String name;

    // FIXME: Revisit locking here to improve concurrency:
    //        -- identifiers, peerId are a shared resource
    //        -- domChain seems to really be 'ribInChain', accessed from netty thread
    //        -- ribOutChain is accessed from LocRibWriter
    //        hence we want to use the two chains concurrently. The problem is their lifecycle in response to errors,
    //        which needs figuring out.
    @GuardedBy("this")
    private DOMTransactionChain domChain;
    // FIXME: This is an invariant once the peer is 'resolved' -- which happens instantaneously for ApplicationPeer.
    //        There are also a number YangInstanceIdentifiers which are tied to it. We want to keep all of them in one
    //        structure for isolation. This could be a separate DTO (JDK16 record) or isolated into an abstract behavior
    //        class.
    @GuardedBy("this")
    PeerId peerId;

    // These seem to be separate
    @GuardedBy("this")
    @VisibleForTesting
    DOMTransactionChain ribOutChain;
    @GuardedBy("this")
    private FluentFuture<? extends CommitInfo> submitted;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
        justification = "False positive on synchronized createDomChain()")
    AbstractPeer(
            final RIB rib,
            final String peerName,
            final String groupId,
            final PeerRole role,
            final @Nullable ClusterIdentifier clusterId,
            final @Nullable AsNumber localAs,
            final IpAddressNoZone neighborAddress,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized,
            final Map<TablesKey, Integer> afiSafisLlGracefulAdvertized) {
        super(rib.getInstanceIdentifier(), groupId, neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized,
                afiSafisLlGracefulAdvertized);
        name = peerName;
        peerRole = role;
        this.clusterId = clusterId;
        this.localAs = localAs;
        this.rib = rib;
        createDomChain();
    }

    AbstractPeer(
            final RIB rib,
            final String peerName,
            final String groupId,
            final PeerRole role,
            final IpAddressNoZone neighborAddress,
            final Set<TablesKey> afiSafisGracefulAdvertized) {
        this(rib, peerName, groupId, role, null, null, neighborAddress,
                rib.getLocalTablesKeys(), afiSafisGracefulAdvertized, Collections.emptyMap());
    }

    final synchronized FluentFuture<? extends CommitInfo> removePeer(final @Nullable YangInstanceIdentifier peerPath) {
        if (peerPath == null) {
            return CommitInfo.emptyFluentFuture();
        }
        LOG.info("Closed per Peer {} removed", peerPath);
        final DOMDataTreeWriteTransaction tx = domChain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, peerPath);
        final FluentFuture<? extends CommitInfo> future = tx.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Peer {} removed", peerPath);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to remove Peer {}", peerPath, throwable);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

    final YangInstanceIdentifier createPeerPath(final PeerId newPeerId) {
        return rib.getYangRibId().node(PEER_NID).node(IdentifierUtils.domPeerId(newPeerId));
    }

    @Override
    public final synchronized PeerId getPeerId() {
        return peerId;
    }

    @Override
    public final PeerRole getRole() {
        return peerRole;
    }

    @Override
    public final PeerRole getFromPeerRole() {
        return getRole();
    }

    @Override
    public final PeerId getFromPeerId() {
        return getPeerId();
    }

    @Override
    public final ClusterIdentifier getFromClusterId() {
        return getClusterId();
    }

    @Override
    public final void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public final BGPErrorHandlingState getBGPErrorHandlingState() {
        return this;
    }

    @Override
    public final BGPAfiSafiState getBGPAfiSafiState() {
        return this;
    }

    @Override
    public final AsNumber getFromPeerLocalAs() {
        return getLocalAs();
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final ClusterIdentifier getClusterId() {
        return clusterId;
    }

    @Override
    public final AsNumber getLocalAs() {
        return localAs;
    }

    @Override
    public synchronized DOMTransactionChain getDomChain() {
        return domChain;
    }

    /**
     * Returns true if route can be send.
     */
    private boolean filterRoutes(final PeerId fromPeer, final TablesKey localTK) {
        return supportsTable(localTK) && !fromPeer.equals(getPeerId());
    }

    @Override
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void initializeRibOut(final RouteEntryDependenciesContainer entryDep,
                    final List<ActualBestPathRoutes<C, S>> routesToStore) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final RIBSupport<C, S> ribSupport = entryDep.getRIBSupport();
        final YangInstanceIdentifier tableRibout = getRibOutIId(ribSupport.tablesKey());
        final boolean addPathSupported = supportsAddPathSupported(ribSupport.getTablesKey());

        final DOMDataTreeWriteTransaction tx = ribOutChain.newWriteOnlyTransaction();
        for (final ActualBestPathRoutes<C, S> initRoute : routesToStore) {
            if (!supportsLLGR() && initRoute.isDepreferenced()) {
                // Stale Long-lived Graceful Restart routes should not be propagated
                continue;
            }

            final PeerId fromPeerId = initRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final MapEntryNode route = initRoute.getRoute();
            final Peer fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
            if (fromPeer == null) {
                LOG.debug("Failed to acquire peer structure for {}, ignoring route {}", fromPeerId, initRoute);
                continue;
            }

            final YangInstanceIdentifier routePath = createRoutePath(ribSupport, tableRibout, initRoute,
                addPathSupported);
            applyExportPolicy(entryDep, fromPeerId, route, routePath, initRoute.getAttributes()).ifPresent(
                attributes -> storeRoute(ribSupport, initRoute, route, routePath, attributes, tx));
        }

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful update commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed update commit", trw);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void refreshRibOut(final RouteEntryDependenciesContainer entryDep,
                final List<StaleBestPathRoute> staleRoutes, final List<AdvertizedRoute<C, S>> newRoutes) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }
        final DOMDataTreeWriteTransaction tx = ribOutChain.newWriteOnlyTransaction();
        final RIBSupport<C, S> ribSupport = entryDep.getRIBSupport();
        deleteRouteRibOut(ribSupport, staleRoutes, tx);
        installRouteRibOut(entryDep, newRoutes, tx);

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful update commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed update commit", trw);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void reEvaluateAdvertizement(final RouteEntryDependenciesContainer entryDep,
                final List<ActualBestPathRoutes<C, S>> routesToStore) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final RIBSupport<C, S> ribSupport = entryDep.getRIBSupport();
        final NodeIdentifierWithPredicates tk = ribSupport.tablesKey();
        final boolean addPathSupported = supportsAddPathSupported(ribSupport.getTablesKey());

        final DOMDataTreeWriteTransaction tx = ribOutChain.newWriteOnlyTransaction();
        for (final ActualBestPathRoutes<C, S> actualBestRoute : routesToStore) {
            final PeerId fromPeerId = actualBestRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final YangInstanceIdentifier tableRibout = getRibOutIId(tk);
            // Stale Long-lived Graceful Restart routes should not be propagated
            if (supportsLLGR() || !actualBestRoute.isDepreferenced()) {
                final YangInstanceIdentifier routePath = createRoutePath(ribSupport, tableRibout, actualBestRoute,
                    addPathSupported);
                final MapEntryNode route = actualBestRoute.getRoute();
                final Optional<ContainerNode> effAttr = applyExportPolicy(entryDep, fromPeerId, route, routePath,
                    actualBestRoute.getAttributes());
                if (effAttr.isPresent()) {
                    storeRoute(ribSupport, actualBestRoute, route, routePath, effAttr.get(), tx);
                    continue;
                }
            }

            deleteRoute(ribSupport, addPathSupported, tableRibout, actualBestRoute, tx);
        }

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        submitted = future;
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful update commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed update commit", trw);
            }
        }, MoreExecutors.directExecutor());
    }

    private Optional<ContainerNode> applyExportPolicy(final RouteEntryDependenciesContainer entryDep,
            final PeerId fromPeerId, final MapEntryNode route, final YangInstanceIdentifier routePath,
            final ContainerNode attrs) {
        final Peer fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
        final RIBSupport<?, ?> ribSupport = entryDep.getRIBSupport();
        final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(fromPeer, this,
            ribSupport.extractRouteKey(route.getIdentifier()), rtCache);

        final Attributes bindingAttrs = ribSupport.attributeFromContainerNode(attrs);
        final Optional<Attributes> optExportAttrs = entryDep.getRoutingPolicies().applyExportPolicies(routeEntry,
            bindingAttrs, entryDep.getAfiSafType());
        if (optExportAttrs.isEmpty()) {
            // Discards route
            return Optional.empty();
        }
        final Attributes exportAttrs = optExportAttrs.orElseThrow();

        // If the same object is returned we can just reuse 'attrs' instead. Since we are in control of lifecycle here,
        // we use identity comparison, as equality is too costly for the common case -- assuming export policy will not
        // churn objects when it does not have to
        return Optional.of(exportAttrs == bindingAttrs ? attrs
            : ribSupport.attributeToContainerNode(routePath.node(ribSupport.routeAttributesIdentifier()), exportAttrs));
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>> void installRouteRibOut(
            final RouteEntryDependenciesContainer entryDep, final List<AdvertizedRoute<C, S>> routes,
            final DOMDataTreeWriteOperations tx) {
        final RIBSupport<C, S> ribSupport = entryDep.getRIBSupport();
        final TablesKey tk = ribSupport.getTablesKey();
        final BGPPeerTracker peerTracker = entryDep.getPeerTracker();
        final boolean addPathSupported = supportsAddPathSupported(tk);
        final YangInstanceIdentifier tableRibout = getRibOutIId(ribSupport.tablesKey());

        for (final AdvertizedRoute<C, S> advRoute : routes) {
            final PeerId fromPeerId = advRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, tk) || !advRoute.isFirstBestPath() && !addPathSupported) {
                continue;
            }
            if (!supportsLLGR() && advRoute.isDepreferenced()) {
                // https://tools.ietf.org/html/draft-uttaro-idr-bgp-persistence-04#section-4.3
                //     o  The route SHOULD NOT be advertised to any neighbor from which the
                //        Long-lived Graceful Restart Capability has not been received.  The
                //        exception is described in the Optional Partial Deployment
                //        Procedure section (Section 4.7).  Note that this requirement
                //        implies that such routes should be withdrawn from any such
                //        neighbor.
                deleteRoute(ribSupport, addPathSupported, tableRibout, advRoute, tx);
                continue;
            }

            final Peer fromPeer = peerTracker.getPeer(fromPeerId);
            final ContainerNode attributes = advRoute.getAttributes();
            if (fromPeer != null && attributes != null) {
                final YangInstanceIdentifier routePath = createRoutePath(ribSupport, tableRibout, advRoute,
                    addPathSupported);
                final MapEntryNode route = advRoute.getRoute();
                applyExportPolicy(entryDep, fromPeerId, route, routePath, attributes).ifPresent(
                    attrs -> storeRoute(ribSupport, advRoute, route, routePath, attrs, tx));
            }
        }
    }

    private static YangInstanceIdentifier createRoutePath(final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier tableRibout, final RouteKeyIdentifier advRoute, final boolean withAddPath) {
        return ribSupport.createRouteIdentifier(tableRibout,
            withAddPath ? advRoute.getAddPathRouteKeyIdentifier() : advRoute.getNonAddPathRouteKeyIdentifier());
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void deleteRouteRibOut(final RIBSupport<C, S> ribSupport, final List<StaleBestPathRoute> staleRoutesIid,
                final DOMDataTreeWriteOperations tx) {
        final YangInstanceIdentifier tableRibout = getRibOutIId(ribSupport.tablesKey());
        final boolean addPathSupported = supportsAddPathSupported(ribSupport.getTablesKey());
        staleRoutesIid.forEach(staleRouteIid
            -> removeRoute(ribSupport, addPathSupported, tableRibout, staleRouteIid, tx));
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>> void storeRoute(
            final RIBSupport<C, S> ribSupport, final RouteKeyIdentifier advRoute, final MapEntryNode route,
            final YangInstanceIdentifier routePath, final ContainerNode effAttr, final DOMDataTreeWriteOperations tx) {
        LOG.debug("Write advRoute {} to peer AdjRibsOut {}", advRoute, getPeerId());
        tx.put(LogicalDatastoreType.OPERATIONAL, routePath, ribSupport.createRoute(route,
            (NodeIdentifierWithPredicates) routePath.getLastPathArgument(), effAttr));
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void removeRoute(final RIBSupport<C, S> ribSupport, final boolean addPathSupported,
                final YangInstanceIdentifier tableRibout, final StaleBestPathRoute staleRouteIid,
                final DOMDataTreeWriteOperations tx) {
        if (addPathSupported) {
            List<NodeIdentifierWithPredicates> staleRoutesIId = staleRouteIid.getAddPathRouteKeyIdentifiers();
            for (final NodeIdentifierWithPredicates id : staleRoutesIId) {
                final YangInstanceIdentifier ribOutTarget = ribSupport.createRouteIdentifier(tableRibout, id);
                LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
            }
        } else {
            if (!staleRouteIid.isNonAddPathBestPathNew()) {
                return;
            }
            final YangInstanceIdentifier ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
                    staleRouteIid.getNonAddPathRouteKeyIdentifier());
            LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
            tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
        }
    }

    // FIXME: why is this different from removeRoute()?
    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>> void deleteRoute(
            final RIBSupport<C, S> ribSupport,  final boolean addPathSupported,
            final YangInstanceIdentifier tableRibout, final AbstractAdvertizedRoute<C, S> advRoute,
            final DOMDataTreeWriteOperations tx) {
        final YangInstanceIdentifier ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
            addPathSupported ? advRoute.getAddPathRouteKeyIdentifier() : advRoute.getNonAddPathRouteKeyIdentifier());
        LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
    }

    // FIXME: make this asynchronous?
    final synchronized void releaseRibOutChain(final boolean isWaitForSubmitted) {
        if (isWaitForSubmitted) {
            if (submitted != null) {
                try {
                    submitted.get();
                } catch (final InterruptedException | ExecutionException throwable) {
                    LOG.error("Write routes failed", throwable);
                }
            }
        }

        if (ribOutChain != null) {
            LOG.info("Closing peer chain {}", getPeerId());
            ribOutChain.close();
            ribOutChain = null;
        }
    }

    final synchronized void createDomChain() {
        if (domChain == null) {
            LOG.info("Creating DOM peer chain {}", getPeerId());
            domChain = rib.createPeerDOMChain(this);
        }
    }

    final synchronized void closeDomChain() {
        if (domChain != null) {
            LOG.info("Closing DOM peer chain {}", getPeerId());
            domChain.close();
            domChain = null;
        }
    }

    boolean supportsLLGR() {
        return false;
    }
}
