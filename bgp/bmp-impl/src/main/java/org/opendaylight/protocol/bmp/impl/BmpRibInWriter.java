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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
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
public final class BmpRibInWriter {
    private static final Logger LOG = LoggerFactory.getLogger(BmpRibInWriter.class);
    private static final QName PEER_ID_QNAME = QName.cachedReference(QName.create(Peer.QNAME, "peer-id"));
    private static final NodeIdentifier PEER_ID = new NodeIdentifier(PEER_ID_QNAME);
    private static final NodeIdentifier ADJRIBIN_PRE = new NodeIdentifier(QName.create(Peer.QNAME, "adj-rib-in-pre-policy"));
    private static final NodeIdentifier ADJRIBIN_POST = new NodeIdentifier(QName.create(Peer.QNAME, "adj-rib-in-post-policy"));
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.FALSE);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE);
    private static final NodeIdentifier TABLES = new NodeIdentifier(Tables.QNAME);

    private static final ContainerNode EMPTY_ADJRIBIN_PRE = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN_PRE)
        .addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private static final ContainerNode EMPTY_ADJRIBIN_POST = Builders.containerBuilder().withNodeIdentifier(ADJRIBIN_POST)
        .addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private final YangInstanceIdentifier ribPath;
    private final DOMTransactionChain chain;
    private final Set<TablesKey> tableTypes;
    private final RIBSupportContextRegistry registry;
    private final Map<TablesKey, TableContext> tablesPre;
    private final Map<TablesKey, TableContext> tablesPost;
    private final YangInstanceIdentifier tablesRootPre;
    private final YangInstanceIdentifier tablesRootPost;


    private BmpRibInWriter(final YangInstanceIdentifier ribPath, final DOMTransactionChain chain,
                           final RIBSupportContextRegistry registry, final Set<TablesKey> tableTypes) {
        this.ribPath = ribPath;
        this.chain = chain;
        this.registry = registry;
        this.tableTypes = tableTypes;

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> pb = Builders.mapEntryBuilder();

        pb.withChild(EMPTY_ADJRIBIN_PRE);
        pb.withChild(EMPTY_ADJRIBIN_POST);

        tx.put(LogicalDatastoreType.OPERATIONAL, ribPath, pb.build());
        LOG.debug("New peer {} structure installed.", ribPath);

        this.tablesRootPre = ribPath.node(EMPTY_ADJRIBIN_PRE.getIdentifier()).node(TABLES);
        this.tablesRootPost = ribPath.node(EMPTY_ADJRIBIN_POST.getIdentifier()).node(TABLES);

        this.tablesPre = createTableInstance(this.tableTypes, this.tablesRootPre, tx, registry).build();
        this.tablesPost = createTableInstance(this.tableTypes, this.tablesRootPost, tx, registry).build();

        tx.submit();
    }

    public static BmpRibInWriter create(@Nonnull final YangInstanceIdentifier ribPath, @Nonnull final DOMTransactionChain chain,
                                        @Nonnull final RIBSupportContextRegistry registry, @Nonnull final Set<TablesKey> tableTypes) {
        return new BmpRibInWriter(Preconditions.checkNotNull(ribPath), Preconditions.checkNotNull(chain),
            Preconditions.checkNotNull(registry), Preconditions.checkNotNull(tableTypes));
    }

    /**
     * Create new table instance
     *
     * @param tableTypes
     * @param newTablesRoot
     * @param tx
     * @param registry
     * @return
     */
    private ImmutableMap.Builder<TablesKey, TableContext> createTableInstance(final Set<TablesKey> tableTypes, final YangInstanceIdentifier newTablesRoot, final DOMDataWriteTransaction tx, final RIBSupportContextRegistry registry) {
        // Now create new table instances, potentially creating their empty entries

        final ImmutableMap.Builder<TablesKey, TableContext> tb = ImmutableMap.builder();
        for (final TablesKey k : tableTypes) {
            final RIBSupportContext rs = registry.getRIBSupportContext(k);
            if (rs == null) {
                LOG.warn("No support for table type {}, skipping it", k);
                continue;
            }
            final YangInstanceIdentifier.InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(newTablesRoot);
            final YangInstanceIdentifier.NodeIdentifierWithPredicates key = RibSupportUtils.toYangTablesKey(k);
            idb.nodeWithKey(key.getNodeType(), key.getKeyValues());
            TableContext ctx = new TableContext(rs, idb.build());
            ctx.clearTable(tx);

            tx.put(LogicalDatastoreType.OPERATIONAL, ctx.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
            LOG.debug("Created table instance {}", ctx.getTableId());
            tb.put(k, ctx);
        }
        return tb;
    }

    public void close() {
        // TODO When close, remove from DS
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
            this.tablesPost.get(k).clearTable(tx);
            this.tablesPre.get(k).clearTable(tx);
        }

        tx.submit();
    }

    void markTableUptodated(final TablesKey tableTypes) {
        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();

        final TableContext ctxPre = this.tablesPre.get(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, ctxPre.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);
        final TableContext ctxPost = this.tablesPost.get(tableTypes);
        tx.merge(LogicalDatastoreType.OPERATIONAL, ctxPost.getTableId().node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_TRUE.getNodeType()), ATTRIBUTES_UPTODATE_TRUE);

        tx.submit();
    }

    void addRoutes(final MpReachNlri nlri, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .bgp.message.rev130919.path.attributes.Attributes attributes, boolean PreAdj) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx;
        if (PreAdj) {
            ctx = this.tablesPre.get(key);
        } else {
            ctx = this.tablesPost.get(key);
        }

        if (ctx == null) {
            LOG.debug("No table for {}, not accepting NLRI {}", key, nlri);
            return;
        }

        final DOMDataWriteTransaction tx = this.chain.newWriteOnlyTransaction();
        ctx.writeRoutes(tx, nlri, attributes);
        LOG.trace("Write routes {}", nlri);
        tx.submit();
    }

    void removeRoutes(final MpUnreachNlri nlri, boolean PreAdj) {
        final TablesKey key = new TablesKey(nlri.getAfi(), nlri.getSafi());
        final TableContext ctx;
        if (PreAdj) {
            ctx = this.tablesPre.get(key);
        } else {
            ctx = this.tablesPost.get(key);
        }
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
