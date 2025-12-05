/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBOUT_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application Peer is a special case of BGP peer. It serves as an interface for user to advertise user routes to ODL
 * and through ODL to other BGP peers.
 *
 * <p>This peer has it's own RIB, where it stores all user routes. This RIB is located in configurational datastore.
 * Routes are added through RESTCONF.
 *
 * <p>They are then processed as routes from any other peer, through AdjRib, EffectiveRib, LocRib and if they are
 * advertised further, through AdjRibOut.
 *
 * <p>For purposed of import policies such as Best Path Selection, application peer needs to have a BGP-ID that is
 * configurable.
 */
public final class ApplicationPeer extends AbstractPeer implements DOMDataTreeChangeListener {
    @FunctionalInterface
    interface RegisterAppPeerListener {
        /**
         * Register Application Peer Change Listener once AdjRibIn has been successfully initialized.
         */
        void register();
    }

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);
    private static final String APP_PEER_GROUP = "application-peers";

    private final LoadingCache<NodeIdentifierWithPredicates, YangInstanceIdentifier> tablesIId =
        CacheBuilder.newBuilder().build(new CacheLoader<NodeIdentifierWithPredicates, YangInstanceIdentifier>() {
            @Override
            public YangInstanceIdentifier load(final NodeIdentifierWithPredicates key) {
                return peerRibOutIId.node(RIBNodeIdentifiers.TABLES_NID).node(key);
            }
        });

    private final YangInstanceIdentifier adjRibsInId;
    private final YangInstanceIdentifier peerRibOutIId;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private EffectiveRibInWriter effectiveRibInWriter;
    private AdjRibInWriter adjRibInWriter;
    private Registration registration;
    private final Set<NodeIdentifierWithPredicates> supportedTables = new HashSet<>();
    private final BGPSessionStateImpl bgpSessionState = new BGPSessionStateImpl();
    private Registration trackerRegistration;
    private YangInstanceIdentifier peerPath;

    public ApplicationPeer(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final ApplicationRibId applicationRibId, final Ipv4AddressNoZone ipAddress, final RIB rib) {
        super(rib, applicationRibId.getValue(), APP_PEER_GROUP, PeerRole.Internal, null, null,
            new IpAddressNoZone(ipAddress), rib.getLocalTablesKeys(), Set.of(), Map.of());
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        peerId = RouterIds.createPeerId(ipAddress);

        final var peerRib = rib.getYangRibId().node(PEER_NID).node(IdentifierUtils.domPeerId(peerId));
        adjRibsInId = peerRib.node(ADJRIBIN_NID).node(TABLES_NID).toOptimized();
        peerRibOutIId = peerRib.node(ADJRIBOUT_NID).node(TABLES_NID).toOptimized();

        createDomChain();
    }

    public synchronized void instantiateServiceInstance(final DataTreeChangeExtension dataTreeChangeService,
            final DOMDataTreeIdentifier appPeerDOMId) {
        setActive(true);
        final var localTables = rib.getLocalTablesKeys();
        for (var localTable : localTables) {
            final var tableSupport = rib.getRibSupportContext().getRIBSupport(localTable);
            if (tableSupport != null) {
                supportedTables.add(tableSupport.tablesKey());
            } else {
                LOG.warn("Ignoring unsupported table {}", localTable);
            }
        }
        setAdvertizedGracefulRestartTableTypes(List.of());

        createDomChain();
        adjRibInWriter = AdjRibInWriter.create(rib.getYangRibId(), PeerRole.Internal, this);
        final RIBSupportContextRegistry context = rib.getRibSupportContext();
        peerPath = createPeerPath(peerId);
        adjRibInWriter = adjRibInWriter.transform(peerId, peerPath, context, localTables, Map.of(),
            () -> {
                synchronized (this) {
                    if (getDomChain() != null) {
                        registration = dataTreeChangeService.registerTreeChangeListener(appPeerDOMId, this);
                    }
                }
            });

        final var chain = rib.createPeerDOMChain();
        chain.addCallback(this);

        effectiveRibInWriter = new EffectiveRibInWriter(this, rib, chain, peerPath, localTables, tableTypeRegistry,
            new ArrayList<>(), rtCache);
        effectiveRibInWriter.init();
        bgpSessionState.registerMessagesCounter(this);
        trackerRegistration = rib.getPeerTracker().registerPeer(this);
    }

    @Override
    public List<RouteTarget> getMemberships() {
        return List.of();
    }

    @Override
    public void onInitialData() {
        // FIXME: we really want to (under a synchronized block) to ensure adj-rib-in is completely empty here.
        //        Unfortunately that bit is already being done somewhere else. The entire the tables are being created
        //        elsewhere and therefore we need to reign in lifecycle first
    }

    /**
     * Routes come from application RIB that is identified by (configurable) name.
     * Each route is pushed into AdjRibsInWriter with it's whole context. In this
     * method, it doesn't matter if the routes are removed or added, this will
     * be determined in LocRib.
     */
    @Override
    public synchronized void onDataTreeChanged(final List<DataTreeCandidate> changes) {
        final DOMTransactionChain chain = getDomChain();
        if (chain == null) {
            LOG.trace("Skipping data changed called to Application Peer. Change : {}", changes);
            return;
        }
        final DOMDataTreeWriteTransaction tx = chain.newWriteOnlyTransaction();
        LOG.debug("Received data change to ApplicationRib {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.debug("Modification Type {}", tc.getRootNode().modificationType());
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument lastArg = path.getLastPathArgument();
            verify(lastArg instanceof NodeIdentifierWithPredicates,
                    "Unexpected type %s in path %s", lastArg.getClass(), path);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
            if (!supportedTables.contains(tableKey)) {
                LOG.trace("Skipping received data change for non supported family {}.", tableKey);
                continue;
            }
            for (final DataTreeCandidateNode child : tc.getRootNode().childNodes()) {
                final PathArgument childIdentifier = child.name();
                final YangInstanceIdentifier tableId = adjRibsInId.node(tableKey).node(childIdentifier);
                switch (child.modificationType()) {
                    case DELETE:
                    case DISAPPEARED:
                        LOG.trace("App peer -> AdjRibsIn path delete: {}", childIdentifier);
                        tx.delete(LogicalDatastoreType.OPERATIONAL, tableId);
                        break;
                    case UNMODIFIED:
                        // No-op
                        break;
                    case SUBTREE_MODIFIED:
                        if (ROUTES_NID.equals(childIdentifier)) {
                            processRoutesTable(child, tableId, tx, tableId);
                        } else {
                            processWrite(child, tableId, tx);
                        }
                        break;
                    case WRITE:
                    case APPEARED:
                        processWrite(child, tableId, tx);
                        break;
                    default:
                        break;
                }
            }
        }
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Successful commit");
            }

            @Override
            public void onFailure(final Throwable trw) {
                LOG.error("Failed commit", trw);
            }
        }, MoreExecutors.directExecutor());
    }

    private static void processWrite(final DataTreeCandidateNode child, final YangInstanceIdentifier tableId,
            final DOMDataTreeWriteTransaction tx) {
        final var dataAfter = child.dataAfter();
        if (dataAfter != null) {
            LOG.trace("App peer -> AdjRibsIn path : {}", tableId);
            LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
            tx.put(LogicalDatastoreType.OPERATIONAL, tableId, dataAfter);
        }
    }

    private synchronized void processRoutesTable(final DataTreeCandidateNode node,
            final YangInstanceIdentifier identifier, final DOMDataTreeWriteTransaction tx,
            final YangInstanceIdentifier routeTableIdentifier) {
        for (var child : node.childNodes()) {
            final YangInstanceIdentifier childIdentifier = identifier.node(child.name());
            switch (child.modificationType()) {
                case DELETE:
                    LOG.trace("App peer -> AdjRibsIn path delete: {}", childIdentifier);
                    tx.delete(LogicalDatastoreType.OPERATIONAL, childIdentifier);
                    break;
                case UNMODIFIED:
                    // No-op
                    break;
                case SUBTREE_MODIFIED:
                    // For be ables to use DELETE when we remove specific routes as we do when we remove the whole
                    // routes, we need to go deeper three levels
                    if (!routeTableIdentifier.equals(childIdentifier.getParent().getParent().getParent())) {
                        processRoutesTable(child, childIdentifier, tx, routeTableIdentifier);
                    } else {
                        processRouteWrite(child, childIdentifier, tx);
                    }
                    break;
                case WRITE:
                    processRouteWrite(child, childIdentifier, tx);
                    break;
                default:
                    break;
            }
        }
    }

    private static void processRouteWrite(final DataTreeCandidateNode child,
            final YangInstanceIdentifier childIdentifier, final DOMDataTreeWriteTransaction tx) {
        final var dataAfter = child.dataAfter();
        if (dataAfter != null) {
            LOG.trace("App peer -> AdjRibsIn path : {}", childIdentifier);
            LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
            tx.put(LogicalDatastoreType.OPERATIONAL, childIdentifier, dataAfter);
        }
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> close() {
        setActive(false);
        if (registration != null) {
            registration.close();
            registration = null;
        }
        if (adjRibInWriter != null) {
            adjRibInWriter.releaseChain();
        }
        if (effectiveRibInWriter != null) {
            effectiveRibInWriter.close();
        }
        final FluentFuture<? extends CommitInfo> future = removePeer(peerPath);
        closeDomChain();
        if (trackerRegistration != null) {
            trackerRegistration.close();
            trackerRegistration = null;
        }
        return future;
    }

    @Override
    public boolean supportsAddPathSupported(final TablesKey tableKey) {
        return false;
    }

    @Override
    public SendReceive getSupportedAddPathTables(final TablesKey tableKey) {
        return null;
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return rib.supportsTable(tableKey);
    }

    @Override
    public YangInstanceIdentifier getRibOutIId(final NodeIdentifierWithPredicates tablekey) {
        return tablesIId.getUnchecked(tablekey);
    }

    @Override
    public void onFailure(final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public BGPSessionState getBGPSessionState() {
        return bgpSessionState;
    }

    @Override
    public BGPTimersState getBGPTimersState() {
        return bgpSessionState;
    }

    @Override
    public BGPTransportState getBGPTransportState() {
        return bgpSessionState;
    }
}
