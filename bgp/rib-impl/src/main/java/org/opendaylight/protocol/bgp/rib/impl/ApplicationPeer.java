/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerStateImpl;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
 * This peer has it's own RIB, where it stores all user routes. This RIB is
 * located in configurational datastore. Routes are added through RESTCONF.
 *
 * They are then processed as routes from any other peer, through AdjRib,
 * EffectiveRib,LocRib and if they are advertised further, through AdjRibOut.
 *
 * For purposed of import policies such as Best Path Selection, application
 * peer needs to have a BGP-ID that is configurable.
 */
public class ApplicationPeer extends BGPPeerStateImpl implements org.opendaylight.protocol.bgp.rib.spi.Peer,
        BGPRouteEntryImportParameters, ClusteredDOMDataTreeChangeListener, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);

    private final byte[] rawIdentifier;
    private final String name;
    private final YangInstanceIdentifier adjRibsInId;
    private final RIB rib;
    private final InstanceIdentifier<AdjRibOut> peerRibOutIId;
    private final KeyedInstanceIdentifier<Peer, PeerKey> peerIId;
    private DOMTransactionChain chain;
    private DOMTransactionChain writerChain;
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
    private final PeerId peerId;
    private AbstractRegistration trackerRegistration;

    @FunctionalInterface
    interface RegisterAppPeerListener {
        /**
         * Register Application Peer Change Listener once AdjRibIn has been successfully initialized.
         */
        void register();
    }

    public ApplicationPeer(final ApplicationRibId applicationRibId, final Ipv4Address ipAddress, final RIB rib) {
        super(rib.getInstanceIdentifier(), "application-peers", new IpAddress(ipAddress),
                rib.getLocalTablesKeys(), Collections.emptySet());
        this.name = applicationRibId.getValue();
        final RIB targetRib = requireNonNull(rib);
        this.rawIdentifier = InetAddresses.forString(ipAddress.getValue()).getAddress();
        final NodeIdentifierWithPredicates peerIId = IdentifierUtils.domPeerId(RouterIds.createPeerId(ipAddress));
        this.adjRibsInId = targetRib.getYangRibId().node(Peer.QNAME).node(peerIId)
                .node(AdjRibIn.QNAME).node(Tables.QNAME);
        this.rib = targetRib;
        this.peerId = RouterIds.createPeerId(ipAddress);
        this.peerIId = getInstanceIdentifier().child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.rib.rev180329.bgp.rib.rib.Peer.class, new PeerKey(this.peerId));
        this.peerRibOutIId = this.peerIId.child(AdjRibOut.class);
    }

    public synchronized void instantiateServiceInstance(final DOMDataTreeChangeService dataTreeChangeService,
            final DOMDataTreeIdentifier appPeerDOMId) {
        setActive(true);
        this.chain = this.rib.createPeerDOMChain(this);
        this.writerChain = this.rib.createPeerDOMChain(this);

        final Set<TablesKey> localTables = this.rib.getLocalTablesKeys();
        localTables.forEach(tablesKey -> {
            this.supportedTables.add(RibSupportUtils.toYangTablesKey(tablesKey));
        });
        setAdvertizedGracefulRestartTableTypes(Collections.emptyList());

        this.adjRibInWriter = AdjRibInWriter.create(this.rib.getYangRibId(), PeerRole.Internal, this.writerChain);
        final RIBSupportContextRegistry context = this.rib.getRibSupportContext();
        final RegisterAppPeerListener registerAppPeerListener = () -> {
            synchronized (this) {
                if (this.chain != null) {
                    this.registration = dataTreeChangeService.registerDataTreeChangeListener(appPeerDOMId, this);
                }
            }
        };
        this.adjRibInWriter = this.adjRibInWriter.transform(this.peerId, context, localTables, Collections.emptyMap(),
                registerAppPeerListener);
        this.effectiveRibInWriter = EffectiveRibInWriter
                .create(this, this.rib, this.rib.createPeerChain(this), this.peerIId, localTables);
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
        if (this.chain == null) {
            LOG.trace("Skipping data changed called to Application Peer. Change : {}", changes);
            return;
        }
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
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
                        LOG.trace("App peer -> AdjRibsIn path delete: {}", childIdentifier);
                        tx.delete(LogicalDatastoreType.OPERATIONAL, tableId);
                        break;
                    case UNMODIFIED:
                        // No-op
                        break;
                    case SUBTREE_MODIFIED:
                        if (EffectiveRibInWriter.TABLE_ROUTES.equals(childIdentifier)) {
                            processRoutesTable(child, tableId, tx, tableId);
                            break;
                        }
                    case WRITE:
                        if (child.getDataAfter().isPresent()) {
                            final NormalizedNode<?, ?> dataAfter = child.getDataAfter().get();
                            LOG.trace("App peer -> AdjRibsIn path : {}", tableId);
                            LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
                            tx.put(LogicalDatastoreType.OPERATIONAL, tableId, dataAfter);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        tx.submit();
    }

    private synchronized void processRoutesTable(final DataTreeCandidateNode node,
            final YangInstanceIdentifier identifier, final DOMDataWriteTransaction tx,
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
                    //For be ables to use DELETE when we remove specific routes as we do when we remove the whole routes,
                    // we need to go deeper three levels
                    if (!routeTableIdentifier.equals(childIdentifier.getParent().getParent().getParent())) {
                        processRoutesTable(child, childIdentifier, tx, routeTableIdentifier);
                        break;
                    }
                case WRITE:
                    if (child.getDataAfter().isPresent()) {
                        final NormalizedNode<?, ?> dataAfter = child.getDataAfter().get();
                        LOG.trace("App peer -> AdjRibsIn path : {}", childIdentifier);
                        LOG.trace("App peer -> AdjRibsIn data : {}", dataAfter);
                        tx.put(LogicalDatastoreType.OPERATIONAL, childIdentifier, dataAfter);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    // FIXME ListenableFuture<?> should be used once closeServiceInstance uses wildcard too
    @Override
    public synchronized ListenableFuture<Void> close() {
        setActive(false);
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
        if (this.effectiveRibInWriter != null) {
            this.effectiveRibInWriter.close();
        }
        final ListenableFuture<Void> future;
        if (this.adjRibInWriter != null) {
            future = this.adjRibInWriter.removePeer();
        } else {
            future = Futures.immediateFuture(null);
        }
        if (this.chain != null) {
            this.chain.close();
            this.chain = null;
        }
        if (this.writerChain != null) {
            this.writerChain.close();
            this.writerChain = null;
        }
        if (this.trackerRegistration != null) {
            this.trackerRegistration.close();
            this.trackerRegistration = null;
        }
        return future;
    }

    @Override
    public byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
    }

    @Override
    public PeerId getPeerId() {
        return this.peerId;
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
    public PeerRole getRole() {
        return PeerRole.Internal;
    }

    @Override
    public ClusterIdentifier getClusterId() {
        return null;
    }

    @Override
    public AsNumber getLocalAs() {
        return null;
    }

    @Override
    public KeyedInstanceIdentifier<Tables, TablesKey> getRibOutIId(final TablesKey tablesKey) {
        return this.tablesIId.getUnchecked(tablesKey);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
        final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Transaction chain {} failed.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }

    @Override
    public BGPErrorHandlingState getBGPErrorHandlingState() {
        return this;
    }

    @Override
    public BGPAfiSafiState getBGPAfiSafiState() {
        return this;
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


    @Override
    public PeerRole getFromPeerRole() {
        return getRole();
    }

    @Override
    public PeerId getFromPeerId() {
        return getPeerId();
    }

    @Override
    public ClusterIdentifier getFromClusterId() {
        return getClusterId();
    }

    @Override
    public AsNumber getFromPeerLocalAs() {
        return null;
    }
}
