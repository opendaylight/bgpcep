/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteTarget;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application Peer is a special case of BGP peer. It serves as an interface
 * for user to advertise user routes to ODL and through ODL to other BGP peers.
 *
 * <p>
 * This peer has it's own RIB, where it stores all user routes. This RIB is
 * located in configurational datastore. Routes are added through RESTCONF.
 *
 * <p>
 * They are then processed as routes from any other peer, through AdjRib,
 * EffectiveRib,LocRib and if they are advertised further, through AdjRibOut.
 *
 * <p>
 * For purposed of import policies such as Best Path Selection, application
 * peer needs to have a BGP-ID that is configurable.
 */
public class ApplicationPeer extends AbstractPeer implements ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);

    private static final String APP_PEER_GROUP = "application-peers";
    private final YangInstanceIdentifier adjRibsInId;
    private final InstanceIdentifier<AdjRibOut> peerRibOutIId;
    private final KeyedInstanceIdentifier<Peer, PeerKey> peerIId;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private EffectiveRibInWriter effectiveRibInWriter;
    private AdjRibInWriter adjRibInWriter;
    private ListenerRegistration<ApplicationPeer> registration;
    private final Set<NodeIdentifierWithPredicates> supportedTables = new HashSet<>();
    private final BGPSessionStateImpl bgpSessionState = new BGPSessionStateImpl();
    private final LoadingCache<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>> tablesIId
            = CacheBuilder.newBuilder()
            .build(new CacheLoader<TablesKey, KeyedInstanceIdentifier<Tables, TablesKey>>() {
                @Override
                public KeyedInstanceIdentifier<Tables, TablesKey> load(final TablesKey tablesKey) {
                    return ApplicationPeer.this.peerRibOutIId.child(Tables.class, tablesKey);
                }
            });
    private Registration trackerRegistration;
    private YangInstanceIdentifier peerPath;

    @Override
    public List<RouteTarget> getMemberships() {
        return Collections.emptyList();
    }

    @FunctionalInterface
    interface RegisterAppPeerListener {
        /**
         * Register Application Peer Change Listener once AdjRibIn has been successfully initialized.
         */
        void register();
    }

    public ApplicationPeer(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final ApplicationRibId applicationRibId, final Ipv4AddressNoZone ipAddress, final RIB rib) {
        super(rib, applicationRibId.getValue(), APP_PEER_GROUP, PeerRole.Internal,
                new IpAddressNoZone(ipAddress), Collections.emptySet());
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        final RIB targetRib = requireNonNull(rib);
        this.rawIdentifier = InetAddresses.forString(ipAddress.getValue()).getAddress();
        this.adjRibsInId = targetRib.getYangRibId().node(PEER_NID)
                .node(IdentifierUtils.domPeerId(RouterIds.createPeerId(ipAddress)))
                .node(ADJRIBIN_NID).node(TABLES_NID);
        this.peerId = RouterIds.createPeerId(ipAddress);
        this.peerIId = getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.rib.rev180329.bgp.rib.rib.Peer.class, new PeerKey(this.peerId));
        this.peerRibOutIId = this.peerIId.child(AdjRibOut.class);
    }

    public synchronized void instantiateServiceInstance(final DOMDataTreeChangeService dataTreeChangeService,
            final DOMDataTreeIdentifier appPeerDOMId) {
        setActive(true);
        final Set<TablesKey> localTables = this.rib.getLocalTablesKeys();
        localTables.forEach(tablesKey -> this.supportedTables.add(RibSupportUtils.toYangTablesKey(tablesKey)));
        setAdvertizedGracefulRestartTableTypes(Collections.emptyList());

        createDomChain();
        this.adjRibInWriter = AdjRibInWriter.create(this.rib.getYangRibId(), PeerRole.Internal, this);
        final RIBSupportContextRegistry context = this.rib.getRibSupportContext();
        final RegisterAppPeerListener registerAppPeerListener = () -> {
            synchronized (this) {
                if (getDomChain() != null) {
                    this.registration = dataTreeChangeService.registerDataTreeChangeListener(appPeerDOMId, this);
                }
            }
        };
        this.peerPath = createPeerPath();
        this.adjRibInWriter = this.adjRibInWriter.transform(this.peerId, this.peerPath, context, localTables,
                Collections.emptyMap(), registerAppPeerListener);
        this.effectiveRibInWriter = new EffectiveRibInWriter(this, this.rib,
                this.rib.createPeerDOMChain(this), this.peerPath, localTables, this.tableTypeRegistry,
                new ArrayList<>(), this.rtCache);
        this.effectiveRibInWriter.init();
        this.bgpSessionState.registerMessagesCounter(this);
        this.trackerRegistration = this.rib.getPeerTracker().registerPeer(this);
    }

    /**
     * Routes come from application RIB that is identified by (configurable) name.
     * Each route is pushed into AdjRibsInWriter with it's whole context. In this
     * method, it doesn't matter if the routes are removed or added, this will
     * be determined in LocRib.
     */
    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        if (getDomChain() == null) {
            LOG.trace("Skipping data changed called to Application Peer. Change : {}", changes);
            return;
        }
        final DOMDataTreeWriteTransaction tx = getDomChain().newWriteOnlyTransaction();
        LOG.debug("Received data change to ApplicationRib {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.debug("Modification Type {}", tc.getRootNode().getModificationType());
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument lastArg = path.getLastPathArgument();
            Verify.verify(lastArg instanceof NodeIdentifierWithPredicates,
                    "Unexpected type %s in path %s", lastArg.getClass(), path);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
            if (!this.supportedTables.contains(tableKey)) {
                LOG.trace("Skipping received data change for non supported family {}.", tableKey);
                continue;
            }
            for (final DataTreeCandidateNode child : tc.getRootNode().getChildNodes()) {
                final PathArgument childIdentifier = child.getIdentifier();
                final YangInstanceIdentifier tableId = this.adjRibsInId.node(tableKey).node(childIdentifier);
                switch (child.getModificationType()) {
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
        if (child.getDataAfter().isPresent()) {
            final NormalizedNode<?, ?> dataAfter = child.getDataAfter().get();
            LOG.trace("App peer -> AdjRibsIn path : {}", tableId);
            LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
            tx.put(LogicalDatastoreType.OPERATIONAL, tableId, dataAfter);
        }
    }

    private synchronized void processRoutesTable(final DataTreeCandidateNode node,
            final YangInstanceIdentifier identifier, final DOMDataTreeWriteTransaction tx,
            final YangInstanceIdentifier routeTableIdentifier) {
        for (final DataTreeCandidateNode child : node.getChildNodes()) {
            final YangInstanceIdentifier childIdentifier = identifier.node(child.getIdentifier());
            switch (child.getModificationType()) {
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
        if (child.getDataAfter().isPresent()) {
            final NormalizedNode<?, ?> dataAfter = child.getDataAfter().get();
            LOG.trace("App peer -> AdjRibsIn path : {}", childIdentifier);
            LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
            tx.put(LogicalDatastoreType.OPERATIONAL, childIdentifier, dataAfter);
        }
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> close() {
        setActive(false);
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
        if (this.adjRibInWriter != null) {
            this.adjRibInWriter.releaseChain();
        }
        if (this.effectiveRibInWriter != null) {
            this.effectiveRibInWriter.close();
        }
        final FluentFuture<? extends CommitInfo> future;
        future = removePeer(this.peerPath);
        closeDomChain();
        if (this.trackerRegistration != null) {
            this.trackerRegistration.close();
            this.trackerRegistration = null;
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
        return this.rib.supportsTable(tableKey);
    }

    @Override
    public KeyedInstanceIdentifier<Tables, TablesKey> getRibOutIId(final TablesKey tablesKey) {
        return this.tablesIId.getUnchecked(tablesKey);
    }

    @Override
    public void onTransactionChainFailed(final DOMTransactionChain chain, final DOMDataTreeTransaction transaction,
            final Throwable cause) {
        LOG.error("Transaction chain {} failed.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain chain, final Transaction transaction,
            final Throwable cause) {
        LOG.error("Transaction chain {} failed.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public BGPSessionState getBGPSessionState() {
        return this.bgpSessionState;
    }

    @Override
    public BGPTimersState getBGPTimersState() {
        return this.bgpSessionState;
    }

    @Override
    public BGPTransportState getBGPTransportState() {
        return this.bgpSessionState;
    }
}
