/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_ATTRIBUTES_QNAME;
import static org.opendaylight.protocol.bmp.impl.app.TablesUtil.BMP_ROUTES_QNAME;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;

@NotThreadSafe
final class TableContext {

    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(BMP_ATTRIBUTES_QNAME);

    private static final InstanceIdentifier<MpReachNlri> MP_REACH_NLRI_II = InstanceIdentifier.create(Update.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path
                    .attributes.Attributes.class).augmentation(Attributes1.class).child(MpReachNlri.class);
    private static final InstanceIdentifier<MpUnreachNlri> MP_UNREACH_NLRI_II = InstanceIdentifier.create(Update.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path
                    .attributes.Attributes.class).augmentation(Attributes2.class).child(MpUnreachNlri.class);
    private static final NodeIdentifier ROUTES_NODE_ID = new NodeIdentifier(BMP_ROUTES_QNAME);

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
        final BindingCodecTreeNode<? extends Route> routeListCodec = tableCodecContext
            .streamChild(Routes.class)
            .streamChild(this.tableSupport.routesCaseClass())
            .streamChild(this.tableSupport.routesContainerClass())
            .streamChild(this.tableSupport.routesListClass());

        this.attributesCodec = routeListCodec.streamChild(Attributes.class)
                .createCachingCodec(this.tableSupport.cacheableAttributeObjects());
        this.reachNlriCodec = tree.getSubtreeCodec(MP_REACH_NLRI_II)
                .createCachingCodec(this.tableSupport.cacheableNlriObjects());
        this.unreachNlriCodec = tree.getSubtreeCodec(MP_UNREACH_NLRI_II)
                .createCachingCodec(this.tableSupport.cacheableNlriObjects());
    }

    YangInstanceIdentifier getTableId() {
        return this.tableId;
    }

    void createTable(final DOMDataWriteTransaction tx) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> tb =
                ImmutableNodes.mapEntryBuilder();
        tb.withNodeIdentifier((NodeIdentifierWithPredicates) this.tableId.getLastPathArgument());
        tb.withChild(EMPTY_TABLE_ATTRIBUTES);

        // tableId is keyed, but that fact is not directly visible from YangInstanceIdentifier, see BUG-2796
        final NodeIdentifierWithPredicates tableKey =
                (NodeIdentifierWithPredicates) this.tableId.getLastPathArgument();
        for (final Map.Entry<QName, Object> e : tableKey.getKeyValues().entrySet()) {
            tb.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        final ChoiceNode routes = this.tableSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", this.tableSupport);

        tx.put(LogicalDatastoreType.OPERATIONAL, this.tableId,
                tb.withChild(ImmutableChoiceNodeBuilder.create(routes).withNodeIdentifier(
                        new NodeIdentifier(TablesUtil.BMP_ROUTES_QNAME)).build()).build());
    }

    void writeRoutes(final DOMDataWriteTransaction tx, final MpReachNlri nlri, final Attributes attributes) {
        final ContainerNode domNlri = serializeReachNlri(nlri);
        final ContainerNode routeAttributes = serializeAttributes(attributes);
        this.tableSupport.putRoutes(tx, this.tableId, domNlri, routeAttributes, ROUTES_NODE_ID);
    }

    void removeRoutes(final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        this.tableSupport.deleteRoutes(tx, this.tableId, serializeUnreachNlri(nlri), ROUTES_NODE_ID);
    }

    private ContainerNode serializeUnreachNlri(final MpUnreachNlri nlri) {
        Preconditions.checkState(this.unreachNlriCodec != null, "MpUnReachNlri codec not available");
        return (ContainerNode) this.unreachNlriCodec.serialize(nlri);
    }

    private ContainerNode serializeReachNlri(final MpReachNlri nlri) {
        Preconditions.checkState(this.reachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) this.reachNlriCodec.serialize(nlri);
    }

    private ContainerNode serializeAttributes(final Attributes pathAttr) {
        Preconditions.checkState(this.attributesCodec != null, "Attributes codec not available");
        final AttributesBuilder a = new AttributesBuilder(pathAttr);
        a.addAugmentation(Attributes1.class, null);
        a.addAugmentation(Attributes2.class, null);
        return (ContainerNode) this.attributesCodec.serialize(a.build());
    }
}
