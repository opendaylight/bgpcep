/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPeer extends BGPPeerStateImpl implements BGPRouteEntryImportParameters, TransactionChainListener,
        DOMTransactionChainListener, Peer, PeerTransactionChain {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPeer.class);
    protected final RIB rib;
    final String name;
    final PeerRole peerRole;
    private final ClusterIdentifier clusterId;
    private final AsNumber localAs;
    @GuardedBy("this")
    private DOMTransactionChain domChain;
    @GuardedBy("this")
    TransactionChain bindingChain;
    byte[] rawIdentifier;
    @GuardedBy("this")
    PeerId peerId;
    private FluentFuture<? extends CommitInfo> submitted;
    RTCClientRouteCache rtCache = new RTCClientRouteCache();

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
        this.name = peerName;
        this.peerRole = role;
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
        final DOMDataTreeWriteTransaction tx = this.domChain.newWriteOnlyTransaction();
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

    synchronized YangInstanceIdentifier createPeerPath() {
        return this.rib.getYangRibId().node(PEER_NID).node(IdentifierUtils.domPeerId(this.peerId));
    }

    @Override
    public final synchronized PeerId getPeerId() {
        return this.peerId;
    }

    @Override
    public final PeerRole getRole() {
        return this.peerRole;
    }

    @Override
    public final synchronized byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
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
    public final void onTransactionChainSuccessful(final TransactionChain chain) {
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
        return this.name;
    }

    @Override
    public final ClusterIdentifier getClusterId() {
        return this.clusterId;
    }

    @Override
    public final AsNumber getLocalAs() {
        return this.localAs;
    }

    @Override
    public synchronized DOMTransactionChain getDomChain() {
        return this.domChain;
    }

    /**
     * Returns true if route can be send.
     */
    private boolean filterRoutes(final PeerId fromPeer, final TablesKey localTK) {
        return supportsTable(localTK) && !fromPeer.equals(getPeerId());
    }

    @Override
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void initializeRibOut(final RouteEntryDependenciesContainer entryDep,
                    final List<ActualBestPathRoutes<C, S, R, I>> routesToStore) {
        if (this.bindingChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final RIBSupport<C, S, R, I> ribSupport = entryDep.getRIBSupport();
        final TablesKey tk = entryDep.getRIBSupport().getTablesKey();
        final boolean addPathSupported = supportsAddPathSupported(tk);

        final WriteTransaction tx = this.bindingChain.newWriteOnlyTransaction();
        for (final ActualBestPathRoutes<C, S, R, I> initializingRoute : routesToStore) {
            if (!supportsLLGR() && initializingRoute.isDepreferenced()) {
                // Stale Long-lived Graceful Restart routes should not be propagated
                continue;
            }

            final PeerId fromPeerId = initializingRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final R route = initializingRoute.getRoute();
            final Peer fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
            if (fromPeer == null) {
                LOG.debug("Failed to acquire peer structure for {}, ignoring route {}", fromPeerId, initializingRoute);
                continue;
            }

            final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(fromPeer,
                    this, route.getRouteKey(), this.rtCache);

            final Optional<Attributes> effAttr = entryDep.getRoutingPolicies()
                    .applyExportPolicies(routeEntry, initializingRoute.getAttributes(), entryDep.getAfiSafType());
            final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout = getRibOutIId(tk);

            effAttr.ifPresent(attributes
                -> storeRoute(ribSupport, addPathSupported, tableRibout, initializingRoute, route, attributes, tx));
        }

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        this.submitted = future;
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
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void refreshRibOut(final RouteEntryDependenciesContainer entryDep,
            final List<StaleBestPathRoute<C, S, R, I>> staleRoutes, final List<AdvertizedRoute<C, S, R, I>> newRoutes) {
        if (this.bindingChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }
        final WriteTransaction tx = this.bindingChain.newWriteOnlyTransaction();
        final RIBSupport<C, S, R, I> ribSupport = entryDep.getRIBSupport();
        deleteRouteRibOut(ribSupport, staleRoutes, tx);
        installRouteRibOut(entryDep, newRoutes, tx);

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        this.submitted = future;
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
    public final synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void reEvaluateAdvertizement(
            final RouteEntryDependenciesContainer entryDep,
            final List<ActualBestPathRoutes<C, S, R, I>> routesToStore) {
        if (this.bindingChain == null) {
            LOG.debug("Session closed, skip changes to peer AdjRibsOut {}", getPeerId());
            return;
        }

        final RIBSupport<C,S,R,I> ribSupport = entryDep.getRIBSupport();
        final TablesKey tk = entryDep.getRIBSupport().getTablesKey();
        final boolean addPathSupported = supportsAddPathSupported(tk);

        final WriteTransaction tx = this.bindingChain.newWriteOnlyTransaction();
        for (final ActualBestPathRoutes<C, S, R, I> actualBestRoute : routesToStore) {
            final PeerId fromPeerId = actualBestRoute.getFromPeerId();
            if (!filterRoutes(fromPeerId, ribSupport.getTablesKey())) {
                continue;
            }

            final R route = actualBestRoute.getRoute();
            final Optional<Attributes> effAttr;
            if (supportsLLGR() || !actualBestRoute.isDepreferenced()) {
                final Peer fromPeer = entryDep.getPeerTracker().getPeer(fromPeerId);
                final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(fromPeer,
                    this, route.getRouteKey(), this.rtCache);
                effAttr = entryDep.getRoutingPolicies()
                        .applyExportPolicies(routeEntry, actualBestRoute.getAttributes(), entryDep.getAfiSafType());
            } else {
                // Stale Long-lived Graceful Restart routes should not be propagated
                effAttr = Optional.empty();
            }

            final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout = getRibOutIId(tk);
            if (effAttr.isPresent()) {
                storeRoute(ribSupport, addPathSupported, tableRibout, actualBestRoute, route, effAttr.get(), tx);
            } else {
                deleteRoute(ribSupport, addPathSupported, tableRibout, actualBestRoute, tx);
            }
        }

        final FluentFuture<? extends CommitInfo> future = tx.commit();
        this.submitted = future;
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

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void installRouteRibOut(
                    final RouteEntryDependenciesContainer entryDep, final List<AdvertizedRoute<C, S, R, I>> routes,
                    final WriteTransaction tx) {
        final TablesKey tk = entryDep.getRIBSupport().getTablesKey();
        final BGPPeerTracker peerTracker = entryDep.getPeerTracker();
        final RIBSupport<C, S, R, I> ribSupport = entryDep.getRIBSupport();
        final BGPRibRoutingPolicy routingPolicies = entryDep.getRoutingPolicies();
        final boolean addPathSupported = supportsAddPathSupported(tk);
        final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout = getRibOutIId(tk);

        for (final AdvertizedRoute<C, S, R, I> advRoute : routes) {
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

            final R route = advRoute.getRoute();
            Optional<Attributes> effAttr = Optional.empty();
            final Peer fromPeer = peerTracker.getPeer(fromPeerId);
            final Attributes attributes = advRoute.getAttributes();
            if (fromPeer != null && attributes != null) {
                final BGPRouteEntryExportParameters routeEntry = new BGPRouteEntryExportParametersImpl(fromPeer,
                        this, route.getRouteKey(), this.rtCache);
                effAttr = routingPolicies.applyExportPolicies(routeEntry, attributes, entryDep.getAfiSafType());
            }
            effAttr.ifPresent(attributes1
                -> storeRoute(ribSupport, addPathSupported, tableRibout, advRoute, route, attributes1, tx));
        }
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void deleteRouteRibOut(
            final RIBSupport<C, S, R, I> ribSupport,
            final List<StaleBestPathRoute<C, S, R, I>> staleRoutesIid,
            final WriteTransaction tx) {
        final TablesKey tk = ribSupport.getTablesKey();
        final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout = getRibOutIId(tk);
        final boolean addPathSupported = supportsAddPathSupported(tk);
        staleRoutesIid.forEach(staleRouteIid
            -> removeRoute(ribSupport, addPathSupported, tableRibout, staleRouteIid, tx));
    }

    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void storeRoute(
                    final RIBSupport<C, S, R, I> ribSupport, final boolean addPathSupported,
                    final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout,
                    final RouteKeyIdentifier<R, I> advRoute, final R route, final Attributes effAttr,
                    final WriteTransaction tx) {
        final InstanceIdentifier<R> ribOut;
        final I newKey;
        if (!addPathSupported) {
            ribOut = ribSupport.createRouteIdentifier(tableRibout, advRoute.getNonAddPathRouteKeyIdentifier());
            newKey = ribSupport.createRouteListKey(route.getRouteKey());
        } else {
            ribOut = ribSupport.createRouteIdentifier(tableRibout, advRoute.getAddPathRouteKeyIdentifier());
            newKey = ribSupport.createRouteListKey(route.getPathId(), route.getRouteKey());
        }

        final R newRoute = ribSupport.createRoute(route, newKey, effAttr);
        LOG.debug("Write advRoute {} to peer AdjRibsOut {}", advRoute, getPeerId());
        tx.put(LogicalDatastoreType.OPERATIONAL, ribOut, newRoute);
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>> void removeRoute(final RIBSupport<C, S, R, I> ribSupport,
            final boolean addPathSupported, final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout,
            final StaleBestPathRoute<C, S, R, I> staleRouteIid, final WriteTransaction tx) {
        if (addPathSupported) {
            List<I> staleRoutesIId = staleRouteIid.getAddPathRouteKeyIdentifiers();
            for (final I id : staleRoutesIId) {
                final InstanceIdentifier<R> ribOutTarget = ribSupport.createRouteIdentifier(tableRibout, id);
                LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
                tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
            }
        } else {
            if (!staleRouteIid.isNonAddPathBestPathNew()) {
                return;
            }
            final InstanceIdentifier<R> ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
                    staleRouteIid.getNonAddPathRouteKeyIdentifier());
            LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
            tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
        }
    }

    // FIXME: why is this different from removeRoute()?
    private <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>> void deleteRoute(
            final RIBSupport<C, S, R, I> ribSupport,  final boolean addPathSupported,
            final KeyedInstanceIdentifier<Tables, TablesKey> tableRibout,
            final AbstractAdvertizedRoute<C, S , R, I> advRoute, final WriteTransaction tx) {
        final InstanceIdentifier<R> ribOutTarget = ribSupport.createRouteIdentifier(tableRibout,
            addPathSupported ? advRoute.getAddPathRouteKeyIdentifier() : advRoute.getNonAddPathRouteKeyIdentifier());
        LOG.trace("Removing {} from transaction for peer {}", ribOutTarget, getPeerId());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ribOutTarget);
    }

    final synchronized void releaseBindingChain() {
        if (this.submitted != null) {
            try {
                this.submitted.get();
            } catch (final InterruptedException | ExecutionException throwable) {
                LOG.error("Write routes failed", throwable);
            }
        }
        closeBindingChain();
    }

    private synchronized void closeBindingChain() {
        if (this.bindingChain != null) {
            LOG.info("Closing peer chain {}", getPeerId());
            this.bindingChain.close();
            this.bindingChain = null;
        }
    }

    final synchronized void createDomChain() {
        if (this.domChain == null) {
            LOG.info("Creating DOM peer chain {}", getPeerId());
            this.domChain = this.rib.createPeerDOMChain(this);
        }
    }

    final synchronized void closeDomChain() {
        if (this.domChain != null) {
            LOG.info("Closing DOM peer chain {}", getPeerId());
            this.domChain.close();
            this.domChain = null;
        }
    }

    boolean supportsLLGR() {
        return false;
    }
}
