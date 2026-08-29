/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteOperations;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.impl.BGPRouteEntryExportParametersImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.AbstractAdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.ActualBestPathRoutes;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteKeyIdentifier;
import org.opendaylight.protocol.bgp.rib.spi.entry.StaleBestPathRoute;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract sealed class AbstractPeer extends BGPPeerStateImpl
        implements BGPRouteEntryImportParameters, Peer, PeerTransactionChain, FutureCallback<Empty>
        permits ApplicationPeer, BGPPeer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeer.class);

    final RTCClientRouteCache rtCache = new RTCClientRouteCache();
    final RIB rib;

    private final ClusterIdentifier clusterId;
    private final @NonNull PeerRole role;
    private final AsNumber localAs;
    private final @NonNull String name;

    // FIXME: Revisit locking here to improve concurrency:
    //        -- identifiers, peerId are a shared resource
    //        -- domChain seems to really be 'ribInChain', accessed from netty thread
    //        -- ribOutChain is accessed from LocRibWriter
    //        hence we want to use the two chains concurrently. The problem is their lifecycle in response to errors,
    //        which needs figuring out.
    @GuardedBy("this")
    private DOMTransactionChain domChain = null;
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

    AbstractPeer(
            final RIB rib,
            final String name,
            final String groupId,
            final PeerRole role,
            final @Nullable ClusterIdentifier clusterId,
            final @Nullable AsNumber localAs,
            final IpAddressNoZone neighborAddress,
            final Set<TablesKey> afiSafisAdvertized,
            final Set<TablesKey> afiSafisGracefulAdvertized,
            final Map<TablesKey, Uint24> afiSafisLlGracefulAdvertized) {
        super(rib.getInstanceIdentifier(), groupId, neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized,
                afiSafisLlGracefulAdvertized);
        this.name = requireNonNull(name);
        this.role = requireNonNull(role);
        this.clusterId = clusterId;
        this.localAs = localAs;
        this.rib = rib;
    }

    final synchronized FluentFuture<? extends CommitInfo> removePeer(final @Nullable YangInstanceIdentifier peerPath) {
        if (peerPath == null) {
            return CommitInfo.emptyFluentFuture();
        }

        LOG.info("Closed per Peer {} removed", peerPath);
        final var tx = domChain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, peerPath);
        final var future = tx.commit();
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
        return role;
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
    public final void onSuccess(final Empty value) {
        LOG.debug("Transaction chain successful");
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
    public final synchronized void initializeRibOut(final RouteEntryDependenciesContainer entryDep,
            final List<ActualBestPathRoutes> routesToStore) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final var ribSupport = entryDep.getRIBSupport();
        final var tableRibout = getRibOutIId(ribSupport.tablesKey());
        final boolean addPathSupported = supportsAddPathSupported(ribSupport.getTablesKey());

        final var tx = ribOutChain.newWriteOnlyTransaction();
        for (var initRoute : routesToStore) {
            if (!supportsLLGR() && initRoute.isDepreferenced()) {
                // Stale Long-lived Graceful Restart routes should not be propagated
                continue;
            }

            final var fromPeerId = initRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final var route = initRoute.getRoute();
            final var fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
            if (fromPeer == null) {
                LOG.debug("Failed to acquire peer structure for {}, ignoring route {}", fromPeerId, initRoute);
                continue;
            }

            final var routePath = createRoutePath(ribSupport, tableRibout, initRoute, addPathSupported);
            final var effAttrs = applyExportPolicy(entryDep, fromPeerId, route, routePath, initRoute.getAttributes());
            if (effAttrs != null) {
                storeRoute(ribSupport, initRoute, route, routePath, effAttrs, tx);
            }
        }

        final var future = tx.commit();
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
    public final synchronized void refreshRibOut(final RouteEntryDependenciesContainer entryDep,
            final List<StaleBestPathRoute> staleRoutes, final List<AdvertizedRoute> newRoutes) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final var peerTracker = entryDep.getPeerTracker();
        final var ribSupport = entryDep.getRIBSupport();
        final var tableRibout = getRibOutIId(ribSupport.tablesKey());
        final var tk = ribSupport.getTablesKey();
        final var addPathSupported = supportsAddPathSupported(tk);

        final var tx = ribOutChain.newWriteOnlyTransaction();

        for (var staleRoute : staleRoutes) {
            removeRoute(ribSupport, addPathSupported, tableRibout, staleRoute, tx);
        }
        for (var newRoute : newRoutes) {
            final var fromPeerId = newRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, tk) || !newRoute.isFirstBestPath() && !addPathSupported) {
                continue;
            }
            if (!supportsLLGR() && newRoute.isDepreferenced()) {
                // https://tools.ietf.org/html/draft-uttaro-idr-bgp-persistence-04#section-4.3
                //     o  The route SHOULD NOT be advertised to any neighbor from which the
                //        Long-lived Graceful Restart Capability has not been received.  The
                //        exception is described in the Optional Partial Deployment
                //        Procedure section (Section 4.7).  Note that this requirement
                //        implies that such routes should be withdrawn from any such
                //        neighbor.
                deleteRoute(ribSupport, addPathSupported, tableRibout, newRoute, tx);
                continue;
            }

            final var fromPeer = peerTracker.getPeer(fromPeerId);
            final var attributes = newRoute.getAttributes();
            if (fromPeer != null && attributes != null) {
                final var routePath = createRoutePath(ribSupport, tableRibout, newRoute, addPathSupported);
                final var route = newRoute.getRoute();
                final var effAttrs = applyExportPolicy(entryDep, fromPeerId, route, routePath, attributes);
                if (effAttrs != null) {
                    storeRoute(ribSupport, newRoute, route, routePath, effAttrs, tx);
                }
            }
        }

        final var future = tx.commit();
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
    public final synchronized void reEvaluateAdvertizement(final RouteEntryDependenciesContainer entryDep,
                final List<ActualBestPathRoutes> routesToStore) {
        if (ribOutChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final var ribSupport = entryDep.getRIBSupport();
        final var tk = ribSupport.tablesKey();
        final boolean addPathSupported = supportsAddPathSupported(ribSupport.getTablesKey());

        final var tx = ribOutChain.newWriteOnlyTransaction();
        for (var actualBestRoute : routesToStore) {
            final var fromPeerId = actualBestRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final var tableRibout = getRibOutIId(tk);
            // Stale Long-lived Graceful Restart routes should not be propagated
            if (supportsLLGR() || !actualBestRoute.isDepreferenced()) {
                final var routePath = createRoutePath(ribSupport, tableRibout, actualBestRoute, addPathSupported);
                final var route = actualBestRoute.getRoute();
                final var effAttrs = applyExportPolicy(entryDep, fromPeerId, route, routePath,
                    actualBestRoute.getAttributes());
                if (effAttrs != null) {
                    storeRoute(ribSupport, actualBestRoute, route, routePath, effAttrs, tx);
                    continue;
                }
            }

            deleteRoute(ribSupport, addPathSupported, tableRibout, actualBestRoute, tx);
        }

        final var future = tx.commit();
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

    private @Nullable ContainerNode applyExportPolicy(final RouteEntryDependenciesContainer entryDep,
            final PeerId fromPeerId, final MapEntryNode route, final YangInstanceIdentifier routePath,
            final ContainerNode attrs) {
        final var fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
        final var ribSupport = entryDep.getRIBSupport();
        final var routeEntry = new BGPRouteEntryExportParametersImpl(fromPeer, this,
            ribSupport.extractRouteKey(route.name()), rtCache);

        final var bindingAttrs = ribSupport.attributeFromContainerNode(attrs);
        final var exportAttrs = entryDep.getRoutingPolicies().applyExportPolicies(routeEntry, bindingAttrs,
            entryDep.getAfiSafType());
        if (exportAttrs == null) {
            // Discards route
            return null;
        }

        // If the same object is returned we can just reuse 'attrs' instead. Since we are in control of lifecycle here,
        // we use identity comparison, as equality is too costly for the common case -- assuming export policy will not
        // churn objects when it does not have to
        return exportAttrs == bindingAttrs ? attrs
            : ribSupport.attributeToContainerNode(routePath.node(ribSupport.routeAttributesIdentifier()), exportAttrs);
    }

    private static YangInstanceIdentifier createRoutePath(final RIBSupport<?, ?> ribSupport,
            final YangInstanceIdentifier tableRibout, final RouteKeyIdentifier advRoute, final boolean withAddPath) {
        return ribSupport.createRouteIdentifier(tableRibout,
            withAddPath ? advRoute.getAddPathRouteKeyIdentifier() : advRoute.getNonAddPathRouteKeyIdentifier());
    }

    @Holding("this")
    private void storeRoute(final RIBSupport<?, ?> ribSupport, final RouteKeyIdentifier advRoute,
            final MapEntryNode route, final YangInstanceIdentifier routePath, final ContainerNode effAttr,
            final DOMDataTreeWriteOperations tx) {
        LOG.debug("Write advRoute {} to peer AdjRibsOut {}", advRoute, getPeerId());
        tx.put(LogicalDatastoreType.OPERATIONAL, routePath, ribSupport.createRoute(route,
            (NodeIdentifierWithPredicates) routePath.getLastPathArgument(), effAttr));
    }

    @Holding("this")
    private void removeRoute(final RIBSupport<?, ?> ribSupport, final boolean addPathSupported,
            final YangInstanceIdentifier tableRibout, final StaleBestPathRoute staleRoute,
            final DOMDataTreeWriteOperations tx) {
        if (addPathSupported) {
            for (var id : staleRoute.getAddPathRouteKeyIdentifiers()) {
                final var ribOutTarget = ribSupport.createRouteIdentifier(tableRibout, id);
                LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
            }
            return;
        }

        if (!staleRoute.isNonAddPathBestPathNew()) {
            return;
        }

        final var ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
                staleRoute.getNonAddPathRouteKeyIdentifier());
        LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
    }

    // FIXME: why is this different from removeRoute()?
    @Holding("this")
    private void deleteRoute(final RIBSupport<?, ?> ribSupport, final boolean addPathSupported,
            final YangInstanceIdentifier tableRibout, final AbstractAdvertizedRoute advRoute,
            final DOMDataTreeWriteOperations tx) {
        final var ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
            addPathSupported ? advRoute.getAddPathRouteKeyIdentifier() : advRoute.getNonAddPathRouteKeyIdentifier());
        LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
    }

    /**
     * Releases the RIB-out chain. Optionally waits for the submitted future before closing.
     * This wait has to be asynchronous, so it does not block, because a potential
     * {@link BGPPeer#onRibOutChainFailed} callback needs the same lock. Assigns {@code null} to
     * {@code ribOutChain} so no further writes can be issued.
     *
     * @param isWaitForSubmitted if true, wait for submitted future before closing binding chain. if false, don't wait.
     */
    @VisibleForTesting
    final synchronized void releaseRibOutChain(final boolean isWaitForSubmitted) {
        // take ownership of the chain
        final var chain = ribOutChain;
        if (chain == null) {
            return;
        }
        ribOutChain = null;

        // take ownership of the last committed transaction
        final var last = submitted;
        submitted = null;

        // decide whether to close the chain immediately: the isDone() check is a pure optimization
        if (isWaitForSubmitted && last != null && !last.isDone()) {
            LOG.trace("Deferring chain close until after {} is done", last);
            last.addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.trace("Successful submitted before chain close");
                    closeRibOutChain(chain);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.error("Write routes failed", throwable);
                }
            }, MoreExecutors.directExecutor());
        } else {
            closeRibOutChain(chain);
        }
    }

    private void closeRibOutChain(final DOMTransactionChain chain) {
        LOG.info("Closing peer chain {}", getPeerId());
        chain.close();
    }

    final synchronized void createDomChain() {
        if (domChain == null) {
            LOG.info("Creating DOM peer chain {}", getPeerId());
            domChain = rib.createPeerDOMChain();
            domChain.addCallback(this);
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
