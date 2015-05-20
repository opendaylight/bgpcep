/*
 *
 *  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bmp.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 21.5.2015.
 */
public class BmpRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(BmpRibInWriter.class);
    private static final QName PEER_ID_QNAME = QName.cachedReference(QName.create(Peer.QNAME, "peer-id"));
    private static final NodeIdentifier PEER_ID = new NodeIdentifier(PEER_ID_QNAME);
    private static final NodeIdentifier ADJRIBIN_PRE = new NodeIdentifier(QName.create(Peer.QNAME, "adj-rib-in-pre-policy"));
    private static final NodeIdentifier ADJRIBIN_POST = new NodeIdentifier(QName.create(Peer.QNAME, "adj-rib-in-post-policy"));
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.FALSE);
    private static final NodeIdentifier TABLES = new NodeIdentifier(Tables.QNAME);

    private static final ContainerNode EMPTY_ADJRIBIN_PRE = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN_PRE)
        .addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private static final ContainerNode EMPTY_ADJRIBIN_POST = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN_POST)
        .addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private final YangInstanceIdentifier ribPath;
    private final DOMTransactionChain chain;
    private final Map<TablesKey, TableContext> tablesPre;
    private final Map<TablesKey, TableContext> tablesPost;
    private final YangInstanceIdentifier tablesRootPre;
    private final YangInstanceIdentifier tablesRootPost;
    private final PeerId peerId;


    private BmpRibInWriter(final YangInstanceIdentifier ribPath, final DOMTransactionChain chain, final PeerId peerId,
                           final YangInstanceIdentifier tablesRootPre, final YangInstanceIdentifier tablesRootPost,
                           final Map<TablesKey, TableContext> tablesPre, final Map<TablesKey, TableContext> tablesPost) {
        this.ribPath = Preconditions.checkNotNull(ribPath);
        this.chain = Preconditions.checkNotNull(chain);
        this.tablesPre = Preconditions.checkNotNull(tablesPre);
        this.tablesPost = Preconditions.checkNotNull(tablesPost);
        this.tablesRootPre = tablesRootPre;
        this.tablesRootPost = tablesRootPost;
        this.peerId = peerId;
    }

    /**
     * Create a new writer using a transaction chain.
     *
     * @param chain transaction chain
     * @return A fresh writer instance
     */
    static BmpRibInWriter create(@Nonnull final YangInstanceIdentifier ribId, @Nonnull final DOMTransactionChain chain) {
        return new BmpRibInWriter(ribId, chain, null, null, null, Collections.<TablesKey, TableContext>emptyMap(),
            Collections.<TablesKey, TableContext>emptyMap());
    }

    /**
     * Transform this writer to a new writer, which is in charge of specified tables.
     * Empty tables are created for new entries and old tables are deleted. Once this
     * method returns, the old instance must not be reasonably used.
     *
     * @param newPeerId  new peer BGP identifier
     * @param registry   RIB extension registry
     * @param tableTypes New tables, must not be null
     * @return New writer
     */
    BmpRibInWriter transform(final PeerId newPeerId, final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final YangInstanceIdentifier newTablesRootPre;
        final YangInstanceIdentifier newTablesRootPost;

        if (!newPeerId.equals(this.peerId)) {
            if (this.peerId != null) {
                // Wipe old peer data completely
                tx.delete(LogicalDatastoreType.OPERATIONAL, this.ribPath.node(Peer.QNAME).node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(Peer.QNAME, PEER_ID_QNAME, this.peerId.getValue())));
            }

            // Install new empty peer structure
            final YangInstanceIdentifier.NodeIdentifierWithPredicates peerKey = new YangInstanceIdentifier.NodeIdentifierWithPredicates(Peer.QNAME, PEER_ID_QNAME, newPeerId);
            final YangInstanceIdentifier newPeerPath = this.ribPath.node(Peer.QNAME).node(peerKey);

            final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> pb = Builders.mapEntryBuilder();
            pb.withNodeIdentifier(peerKey);
            pb.withChild(ImmutableNodes.leafNode(PEER_ID, newPeerId.getValue()));
            pb.withChild(EMPTY_ADJRIBIN_PRE);
            pb.withChild(EMPTY_ADJRIBIN_POST);

            tx.put(LogicalDatastoreType.OPERATIONAL, newPeerPath, pb.build());
            LOG.debug("New peer {} structure installed.", newPeerPath);

            newTablesRootPre = newPeerPath.node(EMPTY_ADJRIBIN_PRE.getIdentifier()).node(TABLES);
            newTablesRootPost = newPeerPath.node(EMPTY_ADJRIBIN_POST.getIdentifier()).node(TABLES);
        } else {
            newTablesRootPre = this.tablesRootPre;
            newTablesRootPost = this.tablesRootPost;
            wipeTable(tx, tableTypes, this.tablesPre);
            wipeTable(tx, tableTypes, this.tablesPost);
        }

        final ImmutableMap.Builder<TablesKey, TableContext> tb_pre = createTableInstance(tableTypes,
            newTablesRootPre, this.tablesPre, tx, registry);
        final ImmutableMap.Builder<TablesKey, TableContext> tb_post = createTableInstance(tableTypes,
            newTablesRootPost, this.tablesPost, tx, registry);

        tx.submit();

        return new BmpRibInWriter(this.ribPath, this.chain, newPeerId, newTablesRootPre, newTablesRootPost,
            tb_pre.build(), tb_post.build());
    }

    /**
     * Wipe tables which are not present in the new types
     *
     * @param tx
     * @param tableTypes
     * @param tables
     */
    private void wipeTable(final DOMDataWriteTransaction tx, final Set<TablesKey> tableTypes, final Map<TablesKey, TableContext> tables) {
        for (final Map.Entry<TablesKey, TableContext> e : tables.entrySet()) {
            if (!tableTypes.contains(e.getKey())) {
                e.getValue().removeTable(tx);
            }
        }
    }

    /**
     * Create new table instance
     *
     * @param tableTypes
     * @param newTablesRoot
     * @param tables
     * @param tx
     * @param registry
     * @return
     */
    private ImmutableMap.Builder<TablesKey, TableContext> createTableInstance(final Set<TablesKey> tableTypes, final YangInstanceIdentifier newTablesRoot, final Map<TablesKey, TableContext> tables, final DOMDataWriteTransaction tx, final RIBSupportContextRegistry registry) {
        // Now create new table instances, potentially creating their empty entries
        final ImmutableMap.Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey k : tableTypes) {
            TableContext ctx = tables.get(k);
            if (ctx == null) {
                final RIBSupportContext rs = registry.getRIBSupportContext(k);
                if (rs == null) {
                    LOG.warn("No support for table type {}, skipping it", k);
                    continue;
                }
                final YangInstanceIdentifier.InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(newTablesRoot);
                final YangInstanceIdentifier.NodeIdentifierWithPredicates key = RibSupportUtils.toYangTablesKey(k);
                idb.nodeWithKey(key.getNodeType(), key.getKeyValues());
                ctx = new TableContext(rs, idb.build());
                ctx.clearTable(tx);
            }
            tx.merge(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
            LOG.debug("Created table instance {}", ctx.getTableId());
            tb.put(k, ctx);
        }
        return tb;
    }

}
