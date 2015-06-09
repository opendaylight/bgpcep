/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.app;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

/**
 * Created by cgasparini on 22.5.2015.
 */
@NotThreadSafe
final class TableContext {

    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.QNAME);

    private static final InstanceIdentifier<MpReachNlri> MP_REACH_NLRI_II = InstanceIdentifier.create(Update.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes.class)
                .augmentation(Attributes1.class)
                .child(MpReachNlri.class);
    private static final InstanceIdentifier<MpUnreachNlri> MP_UNREACH_NLRI_II = InstanceIdentifier.create(Update.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes.class)
            .augmentation(Attributes2.class)
            .child(MpUnreachNlri.class);

    private final YangInstanceIdentifier tableId;
    private final RIBSupport tableSupport;
    private BindingNormalizedNodeCachingCodec<Attributes> attributesCodec;
    private BindingNormalizedNodeCachingCodec<MpReachNlri> reachNlriCodec;
    private BindingNormalizedNodeCachingCodec<MpUnreachNlri> unreachNlriCodec;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    TableContext(final RIBSupport tableSupport, final YangInstanceIdentifier tableId, final BindingCodecTree tree) {
        this.tableSupport = Preconditions.checkNotNull(tableSupport);
        this.tableId = Preconditions.checkNotNull(tableId);
        final BindingCodecTreeNode tableCodecContext = tree.getSubtreeCodec(tableId);
        final BindingCodecTreeNode<? extends Route> routeListCodec = tableCodecContext
            .streamChild(Routes.class)
            .streamChild(this.tableSupport.routesCaseClass())
            .streamChild(this.tableSupport.routesContainerClass())
            .streamChild(this.tableSupport.routesListClass());

        this.attributesCodec = routeListCodec.streamChild(Attributes.class).createCachingCodec(this.tableSupport.cacheableAttributeObjects());
        this.reachNlriCodec = tree.getSubtreeCodec(MP_REACH_NLRI_II).createCachingCodec(this.tableSupport.cacheableNlriObjects());
        this.unreachNlriCodec = tree.getSubtreeCodec(MP_UNREACH_NLRI_II).createCachingCodec(this.tableSupport.cacheableNlriObjects());
    }

    YangInstanceIdentifier getTableId() {
        return this.tableId;
    }

    void createTable(final DOMDataWriteTransaction tx) {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> tb = ImmutableNodes.mapEntryBuilder();
        tb.withNodeIdentifier((YangInstanceIdentifier.NodeIdentifierWithPredicates)tableId.getLastPathArgument());
        tb.withChild(EMPTY_TABLE_ATTRIBUTES);

        // tableId is keyed, but that fact is not directly visible from YangInstanceIdentifier, see BUG-2796
        final YangInstanceIdentifier.NodeIdentifierWithPredicates tableKey = (YangInstanceIdentifier.NodeIdentifierWithPredicates) tableId.getLastPathArgument();
        for (final Map.Entry<QName, Object> e : tableKey.getKeyValues().entrySet()) {
            tb.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        final ChoiceNode routes = this.tableSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", this.tableSupport);
        Verify.verify(Routes.QNAME.equals(routes.getNodeType()), "Empty routes have unexpected identifier %s, expected %s", routes.getNodeType(), Routes.QNAME);

        tx.put(LogicalDatastoreType.OPERATIONAL, this.tableId, tb.withChild(routes).build());
    }

    void removeTable(final DOMDataWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, this.tableId);
    }

    void writeRoutes(final DOMDataWriteTransaction tx, final MpReachNlri nlri, final Attributes attributes) {
        final ContainerNode domNlri = serialiazeReachNlri(nlri);
        final ContainerNode routeAttributes = serializeAttributes(attributes);
        this.tableSupport.putRoutes(tx, tableId, domNlri, routeAttributes);
    }

    void removeRoutes(final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        this.tableSupport.deleteRoutes(tx, tableId, serialiazeUnreachNlri(nlri));
    }

    private ContainerNode serialiazeUnreachNlri(final MpUnreachNlri nlri) {
        Preconditions.checkState(this.unreachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) this.unreachNlriCodec.serialize(nlri);
    }

    private ContainerNode serialiazeReachNlri(final MpReachNlri nlri) {
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