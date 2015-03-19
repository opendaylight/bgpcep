/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
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
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object
 * is attached to each {@link BGPPeer} and {@link ApplicationPeer}.
 */
@NotThreadSafe
final class AdjRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AdjRibInWriter.class);

    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.FALSE);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE);
    private static final QName RIB_ID_QNAME = QName.cachedReference(QName.create(Rib.QNAME, "id"));
    private static final QName AFI_QNAME = QName.cachedReference(QName.create(Tables.QNAME, "afi"));
    private static final QName SAFI_QNAME = QName.cachedReference(QName.create(Tables.QNAME, "safi"));
    private static final QName PEER_ID_QNAME = QName.cachedReference(QName.create(Peer.QNAME, "peer-id"));
    private static final QName PEER_ROLE_QNAME = QName.cachedReference(QName.create(Peer.QNAME, "peer-role"));
    private static final NodeIdentifier ADJRIBIN = new NodeIdentifier(AdjRibIn.QNAME);
    private static final NodeIdentifier PEER_ID = new NodeIdentifier(PEER_ID_QNAME);
    private static final NodeIdentifier PEER_ROLE = new NodeIdentifier(PEER_ROLE_QNAME);
    private static final NodeIdentifier TABLES = new NodeIdentifier(Tables.QNAME);

    // FIXME: is there a utility method to construct this?
    private static final ContainerNode EMPTY_ADJRIBIN = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN).addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();

    private final Map<TablesKey, TableContext> tables;
    private final YangInstanceIdentifier tablesRoot;
    private final YangInstanceIdentifier ribPath;
    private final DOMTransactionChain chain;
    private final Ipv4Address peerId = null;
    private final String role;

    /*
     * FIXME: transaction chain has to be instantiated in caller, so it can terminate us when it fails.
     */
    private AdjRibInWriter(final YangInstanceIdentifier ribPath, final DOMTransactionChain chain, final String role, final YangInstanceIdentifier tablesRoot, final Map<TablesKey, TableContext> tables) {
        this.ribPath = Preconditions.checkNotNull(ribPath);
        this.chain = Preconditions.checkNotNull(chain);
        this.tables = Preconditions.checkNotNull(tables);
        this.role = Preconditions.checkNotNull(role);
        this.tablesRoot = tablesRoot;
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
    static AdjRibInWriter create(@Nonnull final RibKey key, @Nonnull final PeerRole role, @Nonnull final DOMTransactionChain chain) {
        final InstanceIdentifierBuilder b = YangInstanceIdentifier.builder();
        b.node(BgpRib.QNAME);
        b.node(Rib.QNAME);
        b.nodeWithKey(Rib.QNAME, RIB_ID_QNAME, key.getId().getValue());

        return new AdjRibInWriter(b.build(), chain, roleString(role), null, Collections.<TablesKey, TableContext>emptyMap());
    }

    /**
     * Transform this writer to a new writer, which is in charge of specified tables.
     * Empty tables are created for new entries and old tables are deleted. Once this
     * method returns, the old instance must not be reasonably used.
     *
     * @param newPeerId new peer BGP identifier
     * @param registry RIB extension registry
     * @param tableTypes New tables, must not be null
     * @return New writer
     */
    AdjRibInWriter transform(final Ipv4Address newPeerId, final RIBExtensionConsumerContext registry, final Set<TablesKey> tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final YangInstanceIdentifier newTablesRoot;
        if (!newPeerId.equals(this.peerId)) {
            if (this.peerId != null) {
                // Wipe old peer data completely
                tx.delete(LogicalDatastoreType.OPERATIONAL, this.ribPath.node(Peer.QNAME).node(new NodeIdentifierWithPredicates(Peer.QNAME, PEER_ID_QNAME, this.peerId.getValue())));
            }

            // Install new empty peer structure
            final NodeIdentifierWithPredicates peerKey = new NodeIdentifierWithPredicates(Peer.QNAME, PEER_ID_QNAME, newPeerId.getValue());
            final YangInstanceIdentifier newPeerPath = this.ribPath.node(Peer.QNAME).node(peerKey);

            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> pb = Builders.mapEntryBuilder();
            pb.withNodeIdentifier(peerKey);
            pb.withChild(ImmutableNodes.leafNode(PEER_ID, newPeerId.getValue()));
            pb.withChild(ImmutableNodes.leafNode(PEER_ROLE, this.role));
            pb.withChild(EMPTY_ADJRIBIN);

            tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath, pb.build());
            LOG.debug("New peer {} structure installed.", newPeerPath);

            newTablesRoot = newPeerPath.node(EMPTY_ADJRIBIN.getIdentifier()).node(TABLES);
        } else {
            newTablesRoot = this.tablesRoot;

            // Wipe tables which are not present in the new types
            for (final Entry<TablesKey, TableContext> e : this.tables.entrySet()) {
                if (!tableTypes.contains(e.getKey())) {
                    e.getValue().removeTable(tx);
                }
            }
        }

        // Now create new table instances, potentially creating their empty entries
        final Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey k : tableTypes) {
            TableContext ctx = this.tables.get(k);
            if (ctx == null) {
                final RIBSupport rs = registry.getRIBSupport(k);
                if (rs == null) {
                    LOG.warn("No support for table type {}, skipping it", k);
                    continue;
                }

                // We will use table keys very often, make sure they are optimized
                final InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(newTablesRoot);

                // FIXME: use codec to translate the key
                final Map<QName, Object> keyValues = ImmutableMap.<QName, Object>of(AFI_QNAME, BindingReflections.getQName(k.getAfi()), SAFI_QNAME, BindingReflections.getQName(k.getSafi()));
                final NodeIdentifierWithPredicates key = new NodeIdentifierWithPredicates(Tables.QNAME, keyValues);
                idb.nodeWithKey(key.getNodeType(), keyValues);

                ctx = new TableContext(rs, idb.build());
                ctx.clearTable(tx);
            } else {
                tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
            }
            LOG.debug("Created table instance {}", ctx);
            tb.put(k, ctx);
        }

        tx.submit();

        return new AdjRibInWriter(this.ribPath, this.chain, this.role, newTablesRoot, tb.build());
    }

    /**
     * Clean all routes in specified tables
     *
     * @param tableTypes Tables to clean.
     */
    void cleanTables(final Collection<TablesKey> tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        for (final TablesKey k : tableTypes) {
            LOG.debug("Clearing table {}", k);
            this.tables.get(k).clearTable(tx);
        }

        tx.submit();
    }

    void markTablesUptodate(final Collection<TablesKey> tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        for (final TablesKey k : tableTypes) {
            final TableContext ctx = this.tables.get(k);
            tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);
        }

        tx.submit();
    }

    void updateRoutes(final MpReachNlri nlri, final PathAttributes attributes) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.writeRoutes(null, tx, nlri, attributes);
        tx.submit();
    }

    void removeRoutes(final MpUnreachNlri nlri) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx = this.tables.get(key);
        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.removeRoutes(null, tx, nlri);
        tx.submit();
    }
}
