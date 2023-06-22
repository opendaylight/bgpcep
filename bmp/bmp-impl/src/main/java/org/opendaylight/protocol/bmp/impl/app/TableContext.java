/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.app;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_ATTRIBUTES_QNAME;
import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_ROUTES_QNAME;

import java.util.Map;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingDataObjectCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

// This class is NOT thread-safe
final class TableContext {

    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(BMP_ATTRIBUTES_QNAME);
    private static final NodeIdentifier BGP_ROUTES_NODE_ID = new NodeIdentifier(BMP_ROUTES_QNAME);

    private final YangInstanceIdentifier tableId;
    private final RIBSupport tableSupport;
    private final BindingNormalizedNodeCachingCodec<Attributes> attributesCodec;
    private final BindingNormalizedNodeCachingCodec<MpReachNlri> reachNlriCodec;
    private final BindingNormalizedNodeCachingCodec<MpUnreachNlri> unreachNlriCodec;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    TableContext(final RIBSupport tableSupport, final YangInstanceIdentifier tableId, final BindingCodecTree tree) {
        this.tableSupport = requireNonNull(tableSupport);
        this.tableId = requireNonNull(tableId);
        final BindingCodecTreeNode tableCodecContext = tree.getSubtreeCodec(tableId);

        checkState(tableCodecContext instanceof BindingDataObjectCodecTreeNode);
        final BindingDataObjectCodecTreeNode<?> routeListCodec = ((BindingDataObjectCodecTreeNode)tableCodecContext)
            .getStreamChild(Routes.class)
            .getStreamChild(this.tableSupport.routesCaseClass())
            .getStreamChild(this.tableSupport.routesContainerClass())
            .getStreamDataObject(this.tableSupport.routesListClass());

        attributesCodec = routeListCodec.getStreamDataObject(Attributes.class)
                .createCachingCodec(this.tableSupport.cacheableAttributeObjects());

        final var updateAttributesCodec = tree.getStreamChild(Update.class)
                .getStreamDataObject(Attributes.class);
        reachNlriCodec = updateAttributesCodec.getStreamChild(AttributesReach.class)
            .getStreamDataObject(MpReachNlri.class)
            .createCachingCodec(this.tableSupport.cacheableNlriObjects());
        unreachNlriCodec = updateAttributesCodec.getStreamChild(AttributesUnreach.class)
            .getStreamDataObject(MpUnreachNlri.class)
            .createCachingCodec(this.tableSupport.cacheableNlriObjects());
    }

    YangInstanceIdentifier getTableId() {
        return tableId;
    }

    void createTable(final DOMDataTreeWriteTransaction tx) {
        final var tb = ImmutableNodes.mapEntryBuilder()
            .withNodeIdentifier((NodeIdentifierWithPredicates) tableId.getLastPathArgument())
            .withChild(EMPTY_TABLE_ATTRIBUTES);

        // tableId is keyed, but that fact is not directly visible from YangInstanceIdentifier, see BUG-2796
        final var tableKey = (NodeIdentifierWithPredicates) tableId.getLastPathArgument();
        for (final Map.Entry<QName, Object> e : tableKey.entrySet()) {
            tb.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        tx.put(LogicalDatastoreType.OPERATIONAL, tableId,
                tb.withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(new NodeIdentifier(TablesUtil.BMP_ROUTES_QNAME))
                    .build())
                .build());
    }

    void writeRoutes(final DOMDataTreeWriteTransaction tx, final MpReachNlri nlri, final Attributes attributes) {
        final ContainerNode domNlri = serializeReachNlri(nlri);
        final ContainerNode routeAttributes = serializeAttributes(attributes);
        tableSupport.putRoutes(tx, tableId, domNlri, routeAttributes, BGP_ROUTES_NODE_ID);
    }

    void removeRoutes(final DOMDataTreeWriteTransaction tx, final MpUnreachNlri nlri) {
        tableSupport.deleteRoutes(tx, tableId, serializeUnreachNlri(nlri), BGP_ROUTES_NODE_ID);
    }

    private ContainerNode serializeUnreachNlri(final MpUnreachNlri nlri) {
        checkState(unreachNlriCodec != null, "MpUnReachNlri codec not available");
        return (ContainerNode) unreachNlriCodec.serialize(nlri);
    }

    private ContainerNode serializeReachNlri(final MpReachNlri nlri) {
        checkState(reachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) reachNlriCodec.serialize(nlri);
    }

    private ContainerNode serializeAttributes(final Attributes pathAttr) {
        checkState(attributesCodec != null, "Attributes codec not available");
        final AttributesBuilder a = new AttributesBuilder(pathAttr);
        a.removeAugmentation(AttributesReach.class);
        a.removeAugmentation(AttributesUnreach.class);
        return (ContainerNode) attributesCodec.serialize(a.build());
    }
}
