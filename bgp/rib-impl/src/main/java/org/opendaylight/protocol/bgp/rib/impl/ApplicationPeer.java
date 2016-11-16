/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.net.InetAddresses;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.BGPPeerStats;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.BGPPeerStatsImpl;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
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
public class ApplicationPeer extends BGPPeerState implements AutoCloseable, org.opendaylight.protocol.bgp.rib.spi.Peer,
    ClusteredDOMDataTreeChangeListener, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationPeer.class);

    private final byte[] rawIdentifier;
    private final String name;
    private final YangInstanceIdentifier adjRibsInId;
    private final Ipv4Address neighborAddress;
    private final RIB rib;
    private final YangInstanceIdentifier peerIId;
    private final AbstractRegistration stateRegistration;
    private DOMTransactionChain chain;
    private DOMTransactionChain writerChain;
    private EffectiveRibInWriter effectiveRibInWriter;
    private AdjRibInWriter adjRibInWriter;

    public ApplicationPeer(final ApplicationRibId applicationRibId, final IpAddress neighborAddress, final RIB rib,
        final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        super(neighborAddress, OpenConfigMappingUtil.toAfiSafiTypeSet(rib.getLocalTablesKeys(), tableTypeRegistry), tableTypeRegistry);
        this.name = applicationRibId.getValue();
        final RIB targetRib = Preconditions.checkNotNull(rib);
        this.neighborAddress = neighborAddress.getIpv4Address();
        this.rawIdentifier = InetAddresses.forString(this.neighborAddress.getValue()).getAddress();
        final NodeIdentifierWithPredicates peerId = IdentifierUtils.domPeerId(RouterIds.createPeerId(this.neighborAddress));
        this.peerIId = targetRib.getYangRibId().node(Peer.QNAME).node(peerId);
        this.adjRibsInId = this.peerIId.node(AdjRibIn.QNAME).node(Tables.QNAME);
        this.rib = targetRib;
        this.stateRegistration = this.rib.registerNeighbor(this.neighborState, null);
    }

    public void instantiateServiceInstance() {
        this.chain = this.rib.createPeerChain(this);
        this.writerChain = this.rib.createPeerChain(this);

        final Optional<SimpleRoutingPolicy> simpleRoutingPolicy = Optional.of(SimpleRoutingPolicy.AnnounceNone);
        final PeerId peerId = RouterIds.createPeerId(this.neighborAddress);
        final Set<TablesKey> localTables = this.rib.getLocalTablesKeys();
        localTables.forEach(tablesKey -> {
            final ExportPolicyPeerTracker exportTracker = this.rib.getExportPolicyPeerTracker(tablesKey);
            if (exportTracker != null) {
                exportTracker.registerPeer(peerId, null, this.peerIId, PeerRole.Internal, simpleRoutingPolicy);
            }
        });
        final Set<Class<? extends AfiSafiType>> receivedAfiSafis = localTables.stream()
            .map(tablesKey -> OpenConfigMappingUtil.toAfiSafi(tablesKey, Collections.emptyList(), this.tableTypeRegistry))
            .filter(Optional::isPresent).map(optional-> optional.get().getAfiSafiName()).collect(Collectors.toSet());
        this.neighborState.setActiveAfiSafi(receivedAfiSafis, Collections.emptySet());

        this.adjRibInWriter = AdjRibInWriter.create(this.rib.getYangRibId(), PeerRole.Internal, simpleRoutingPolicy, this.writerChain, this.neighborState);
        final RIBSupportContextRegistry context = this.rib.getRibSupportContext();
        this.adjRibInWriter = this.adjRibInWriter.transform(peerId, context, localTables, Collections.emptyMap());
        final BGPPeerStats peerStats = new BGPPeerStatsImpl(this.name, localTables);
        this.effectiveRibInWriter = EffectiveRibInWriter.create(this.rib.getService(), this.rib.createPeerChain(this), this.peerIId,
            this.rib.getImportPolicyPeerTracker(), context, PeerRole.Internal, peerStats.getEffectiveRibInRouteCounters(),
            peerStats.getAdjRibInRouteCounters(), this.tableTypeRegistry, this.neighborState);
    }

    /**
     * Routes come from application RIB that is identified by (configurable) name.
     * Each route is pushed into AdjRibsInWriter with it's whole context. In this
     * method, it doesn't matter if the routes are removed or added, this will
     * be determined in LocRib.
     */
    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        LOG.debug("Received data change to ApplicationRib {}", changes);
        for (final DataTreeCandidate tc : changes) {
            LOG.debug("Modification Type {}", tc.getRootNode().getModificationType());
            final YangInstanceIdentifier path = tc.getRootPath();
            final PathArgument lastArg = path.getLastPathArgument();
            Verify.verify(lastArg instanceof NodeIdentifierWithPredicates, "Unexpected type %s in path %s", lastArg.getClass(), path);
            final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) lastArg;
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
                        final NormalizedNode<?,?> dataAfter = child.getDataAfter().get();
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

    /**
     * Applies modification under table routes based on modification type instead of only put. BUG 4438
     * @param node
     * @param identifier
     * @param tx
     * @param routeTableIdentifier
     */
    private void processRoutesTable(final DataTreeCandidateNode node, final YangInstanceIdentifier identifier,
            final DOMDataWriteTransaction tx, final YangInstanceIdentifier routeTableIdentifier) {
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
                    final NormalizedNode<?,?> dataAfter = child.getDataAfter().get();
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

    @Override
    public void close() {
        this.stateRegistration.close();
        if(this.effectiveRibInWriter != null) {
            this.effectiveRibInWriter.close();
        }
        if(this.adjRibInWriter != null) {
            this.adjRibInWriter.removePeer();
        }
        if(this.chain != null) {
            this.chain.close();
        }
        if(this.writerChain != null) {
            this.writerChain.close();
        }
    }

    @Override
    public byte[] getRawIdentifier() {
        return Arrays.copyOf(this.rawIdentifier, this.rawIdentifier.length);
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        LOG.error("Transaction chain failed.", cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successfull.", chain);
    }
}
