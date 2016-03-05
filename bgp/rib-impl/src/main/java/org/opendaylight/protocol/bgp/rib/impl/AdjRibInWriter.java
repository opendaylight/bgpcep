/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object
 * is attached to each {@link BGPPeer} and {@link ApplicationPeer}.
 */
@NotThreadSafe
final class AdjRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInWriter.class);
    @VisibleForTesting
    static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.FALSE);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE);
    @VisibleForTesting
    static final QName PEER_ID_QNAME = QName.create(Peer.QNAME, "peer-id").intern();
    private static final QName PEER_ROLE_QNAME = QName.create(Peer.QNAME, "peer-role").intern();
    private static final NodeIdentifier ADJRIBIN = new NodeIdentifier(AdjRibIn.QNAME);
    private static final NodeIdentifier ADJRIBOUT = new NodeIdentifier(AdjRibOut.QNAME);
    private static final NodeIdentifier EFFRIBIN = new NodeIdentifier(EffectiveRibIn.QNAME);
    private static final NodeIdentifier PEER_ID = new NodeIdentifier(PEER_ID_QNAME);
    private static final NodeIdentifier PEER_ROLE = new NodeIdentifier(PEER_ROLE_QNAME);
    private static final NodeIdentifier PEER_TABLES = new NodeIdentifier(SupportedTables.QNAME);
    private static final NodeIdentifier TABLES = new NodeIdentifier(Tables.QNAME);
    private static final QName SEND_RECEIVE = QName.create(SupportedTables.QNAME, "send-receive").intern();

    // FIXME: is there a utility method to construct this?
    private static final ContainerNode EMPTY_ADJRIBIN = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN).addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private static final ContainerNode EMPTY_EFFRIBIN = Builders.containerBuilder().withNodeIdentifier(EFFRIBIN).addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private static final ContainerNode EMPTY_ADJRIBOUT = Builders.containerBuilder().withNodeIdentifier(ADJRIBOUT).addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();

    private final Map<TablesKey, TableContext> tables;
    private final YangInstanceIdentifier peerPath;
    private final YangInstanceIdentifier ribPath;
    private final DOMTransactionChain chain;
    private final PeerId peerId;
    private final String role;

    private AdjRibInWriter(final YangInstanceIdentifier ribPath, final DOMTransactionChain chain, final PeerId peerId, final String role, final YangInstanceIdentifier peerPath, final Map<TablesKey, TableContext> tables) {
        this.ribPath = Preconditions.checkNotNull(ribPath);
        this.chain = Preconditions.checkNotNull(chain);
        this.tables = Preconditions.checkNotNull(tables);
        this.role = Preconditions.checkNotNull(role);
        this.peerPath = peerPath;
        this.peerId = peerId;
    }

    // We could use a codec, but this should be fine, too
    private static String roleString(final PeerRole role) {
        switch (role) {
        case Ebgp:
            return "ebgp";
        case Ibgp:
            return "ibgp";
        case RrClient:
            return "rr-client";
        case Internal:
            return "internal";
        default:
            throw new IllegalArgumentException("Unhandled role " + role);
        }
    }

    /**
     * Create a new writer using a transaction chain.
     *
     * @param role peer's role
     * @param chain transaction chain
     * @return A fresh writer instance
     */
    static AdjRibInWriter create(@Nonnull final YangInstanceIdentifier ribId, @Nonnull final PeerRole role, @Nonnull final DOMTransactionChain chain) {
        return new AdjRibInWriter(ribId, chain, null, roleString(role), null, Collections.<TablesKey, TableContext>emptyMap());
    }

    /**
     * Transform this writer to a new writer, which is in charge of specified tables.
     * Empty tables are created for new entries and old tables are deleted. Once this
     * method returns, the old instance must not be reasonably used.
     *
     * @param newPeerId new peer BGP identifier
     * @param registry RIB extension registry
     * @param tableTypes New tables, must not be null
     * @param addPathTablesType
     * @return New writer
     */
    AdjRibInWriter transform(final PeerId newPeerId, final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes,
        final List<AddressFamilies> addPathTablesType, final boolean isAppPeer) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final YangInstanceIdentifier newPeerPath;
        newPeerPath = createEmptyPeerStructure(newPeerId, isAppPeer, tx);
        final ImmutableMap<TablesKey, TableContext> tb = createNewTableInstances(newPeerPath, isAppPeer, registry, tableTypes, addPathTablesType, tx);
        tx.submit();

        return new AdjRibInWriter(this.ribPath, this.chain, newPeerId, this.role, newPeerPath, tb);
    }

    /**
     * Create new table instances, potentially creating their empty entries
     * @param newPeerPath
     * @param isAppPeer
     * @param registry
     * @param tableTypes
     * @param addPathTablesType
     *@param tx  @return
     */
    private ImmutableMap<TablesKey, TableContext> createNewTableInstances(final YangInstanceIdentifier newPeerPath, final boolean isAppPeer,
        final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes, final List<AddressFamilies> addPathTablesType,
        final DOMDataWriteTransaction tx) {

        final Map<TablesKey, SendReceive> addPathTableMaps = mapTableTypesFamilies(addPathTablesType);
        final Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey tableKey : tableTypes) {
            final RIBSupportContext rs = registry.getRIBSupportContext(tableKey);
            // TODO: Use returned value once Instance Identifier builder allows for it.
            final NodeIdentifierWithPredicates instanceIdentifierKey = RibSupportUtils.toYangTablesKey(tableKey);
            if (rs == null) {
                LOG.warn("No support for table type {}, skipping it", tableKey);
                continue;
            }
            installAdjRibsOutTables(isAppPeer, newPeerPath, rs, instanceIdentifierKey, tableKey, addPathTableMaps.get(tableKey), tx);
            installAdjRibInTables(newPeerPath, tableKey, rs, instanceIdentifierKey, tx, tb);
        }
        return tb.build();
    }

    private Map<TablesKey, SendReceive> mapTableTypesFamilies(final List<AddressFamilies> addPathTablesType) {
        return Collections.unmodifiableMap(addPathTablesType.stream().collect(Collectors.toMap(af -> new TablesKey(af.getAfi(), af.getSafi()),
            af -> af.getSendReceive())));
    }

    private void installAdjRibInTables(final YangInstanceIdentifier newPeerPath, final TablesKey tableKey, final RIBSupportContext rs,
        final NodeIdentifierWithPredicates instanceIdentifierKey, final DOMDataWriteTransaction tx, final Builder<TablesKey, TableContext> tb) {
        // We will use table keys very often, make sure they are optimized
        final InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(newPeerPath.node(EMPTY_ADJRIBIN.getIdentifier()).node(TABLES));
        idb.nodeWithKey(instanceIdentifierKey.getNodeType(), instanceIdentifierKey.getKeyValues());

        TableContext ctx = new TableContext(rs, idb.build());
        ctx.createEmptyTableStructure(tx);

        tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
        LOG.debug("Created table instance {}", ctx.getTableId());
        tb.put(tableKey, ctx);
    }

    private void installAdjRibsOutTables(final boolean isAppPeer, final YangInstanceIdentifier newPeerPath, final RIBSupportContext rs,
        final NodeIdentifierWithPredicates instanceIdentifierKey, final TablesKey tableKey, final SendReceive sendReceive,
        final DOMDataWriteTransaction tx) {
        if (!isAppPeer) {
            final NodeIdentifierWithPredicates supTablesKey = RibSupportUtils.toYangKey(SupportedTables.QNAME, tableKey);
            final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> tt = Builders.mapEntryBuilder().withNodeIdentifier(supTablesKey);
            for (final Entry<QName, Object> e : supTablesKey.getKeyValues().entrySet()) {
                tt.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
            }
            if(sendReceive != null) {
                tt.withChild(ImmutableNodes.leafNode(SEND_RECEIVE, sendReceive.toString().toLowerCase()));
            }
            tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath.node(PEER_TABLES).node(supTablesKey), tt.build());
            rs.createEmptyTableStructure(tx, newPeerPath.node(EMPTY_ADJRIBOUT.getIdentifier()).node(TABLES).node(instanceIdentifierKey));
        }
    }

    private YangInstanceIdentifier createEmptyPeerStructure(final PeerId newPeerId, final boolean isAppPeer, final DOMDataWriteTransaction tx) {
        final NodeIdentifierWithPredicates peerKey = IdentifierUtils.domPeerId(newPeerId);
        final YangInstanceIdentifier newPeerPath = this.ribPath.node(Peer.QNAME).node(peerKey);

        tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath, peerSkeleton(peerKey, newPeerId.getValue(), isAppPeer));
        LOG.debug("New peer {} structure installed.", newPeerPath);
        return newPeerPath;
    }

    @VisibleForTesting
    MapEntryNode peerSkeleton(final NodeIdentifierWithPredicates peerKey, final String peerId, final boolean isAppPeer) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> pb = Builders.mapEntryBuilder();
        pb.withNodeIdentifier(peerKey);
        pb.withChild(ImmutableNodes.leafNode(PEER_ID, peerId));
        pb.withChild(ImmutableNodes.leafNode(PEER_ROLE, this.role));
        pb.withChild(ImmutableMapNodeBuilder.create().withNodeIdentifier(PEER_TABLES).build());
        pb.withChild(EMPTY_ADJRIBIN);
        pb.withChild(EMPTY_EFFRIBIN);
        if (!isAppPeer) {
            pb.withChild(EMPTY_ADJRIBOUT);
        }
        return pb.build();
    }

    void removePeer() {
        if(this.peerPath != null) {
            final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL, this.peerPath);

            try {
                tx.submit().checkedGet();
            } catch (final TransactionCommitFailedException e) {
                LOG.debug("Failed to remove Peer {}", this.peerPath, e);
            }
        }
    }

    void markTableUptodate(final TablesKey tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final TableContext ctx = this.tables.get(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);

        tx.submit();
    }

    void updateRoutes(final MpReachNlri nlri, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes attributes) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.writeRoutes(tx, nlri, attributes);
        LOG.trace("Write routes {}", nlri);
        tx.submit();
    }

    void removeRoutes(final MpUnreachNlri nlri) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }
        LOG.trace("Removing routes {}", nlri);
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.removeRoutes(tx, nlri);
        tx.submit();
    }

}
