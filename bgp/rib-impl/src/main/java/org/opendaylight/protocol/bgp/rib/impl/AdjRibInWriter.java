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
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
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

/**
 * Writer of Adjacency-RIB-In for a single peer. An instance of this object
 * is attached to each {@link BGPPeer} and {@link ApplicationPeer}.
 */
@NotThreadSafe
final class AdjRibInWriter {
    // FIXME: is there a utility method to construct this?
    private static final ContainerNode EMPTY_ADJRIBIN = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(AdjRibIn.QNAME)).addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build()).build();
    private static final ContainerNode EMPTY_ATTRIBUTES = ImmutableNodes.containerNode(Attributes.QNAME);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_FALSE = ImmutableNodes.leafNode(QName.create(Attributes.QNAME, "uptodate"), Boolean.FALSE);
    private static final LeafNode<Boolean> ATTRIBUTES_UPTODATE_TRUE = ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE);
    private static final QName AFI_QNAME = QName.create(Tables.QNAME, "afi");
    private static final QName SAFI_QNAME = QName.create(Tables.QNAME, "safi");

    private final Map<TablesKey, YangInstanceIdentifier> tables;
    private final YangInstanceIdentifier adjRibInRoot;
    private final DOMTransactionChain chain;

    /*
     * FIXME: transaction chain has to be instantiated in caller, so it can terminate us when it fails.
     */
    private AdjRibInWriter(final DOMTransactionChain chain, final YangInstanceIdentifier adjRibInRoot, final Map<TablesKey, YangInstanceIdentifier> tables) {
        this.chain = Preconditions.checkNotNull(chain);
        this.adjRibInRoot = Preconditions.checkNotNull(adjRibInRoot);
        this.tables = Preconditions.checkNotNull(tables);
    }

    static AdjRibInWriter create(final DOMTransactionChain chain, final YangInstanceIdentifier peer) {
        // Not used often, no need to optimize it via builder
        final YangInstanceIdentifier adjRibInRoot = peer.node(EMPTY_ADJRIBIN.getIdentifier());

        // Create top-level AdjRibIn with an empty table list
        final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, adjRibInRoot, EMPTY_ADJRIBIN);
        tx.submit();

        return new AdjRibInWriter(chain, adjRibInRoot, Collections.<TablesKey, YangInstanceIdentifier>emptyMap());
    }

    private static MapEntryNode emptyTable(final NodeIdentifierWithPredicates key) {
        return ImmutableNodes.mapEntryBuilder().withNodeIdentifier(key).withChild(EMPTY_ATTRIBUTES).build();
    }

    // Only the result may be used after this method returns
    AdjRibInWriter changeTableTypes(final Set<TablesKey> tableTypes) {
        if (tableTypes.equals(tables.keySet())) {
            return this;
        }

        final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

        // Wipe tables which are not present in the new types
        for (Entry<TablesKey, YangInstanceIdentifier> e : tables.entrySet()) {
            if (!tableTypes.contains(e.getKey())) {
                tx.delete(LogicalDatastoreType.OPERATIONAL, e.getValue());
            }
        }

        final Builder<TablesKey, YangInstanceIdentifier> tb = ImmutableMap.builder();
        for (TablesKey k : tableTypes) {
            YangInstanceIdentifier id = tables.get(k);
            if (id == null) {
                // We will use table keys very often, make sure they are optimized
                final InstanceIdentifierBuilder idb = YangInstanceIdentifier.builder(adjRibInRoot);

                // FIXME: use codec to translate the key
                final Map<QName, Object> keyValues = ImmutableMap.<QName, Object>of(AFI_QNAME, BindingReflections.getQName(k.getAfi()), SAFI_QNAME, BindingReflections.getQName(k.getSafi()));
                final NodeIdentifierWithPredicates key = new NodeIdentifierWithPredicates(Tables.QNAME, keyValues);
                idb.nodeWithKey(key.getNodeType(), keyValues);

                id = idb.build();

                tx.put(LogicalDatastoreType.OPERATIONAL, id, emptyTable(key));
            } else {
                tx.merge(LogicalDatastoreType.OPERATIONAL, id.node(Attributes.QNAME).node(ATTRIBUTES_UPTODATE_FALSE.getNodeType()), ATTRIBUTES_UPTODATE_FALSE);
            }

            tb.put(k, id);
        }

        tx.submit();

        return new AdjRibInWriter(chain, adjRibInRoot, tb.build());
    }

    /**
     * Clean all routes in specified tables
     *
     * @param tableTypes Tables to clean.
     */
    void cleanTables(final Set<TablesKey> tableTypes) {
        final DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

        for (TablesKey k : tableTypes) {
            final YangInstanceIdentifier id = tables.get(k);
            tx.put(LogicalDatastoreType.OPERATIONAL, id, emptyTable((NodeIdentifierWithPredicates)id.getLastPathArgument()));
        }

        tx.submit();
    }
}
