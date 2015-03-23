/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ExtendedCommunity;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

/**
 * A context for a single RIB table instance. It is always bound to a particular {@link AdjRibInWriter}.
 *
 * FIXME: need a better name once we local-rib and rib-out contexts
 */
@NotThreadSafe
final class TableContext {
    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.QNAME);
    private static final Set<Class<? extends DataObject>> ATTRIBUTE_CACHEABLES;

    private final BindingCodecTreeFactory codecFactory;

    static {
        final Builder<Class<? extends DataObject>> acb = ImmutableSet.builder();
        acb.add(Aggregator.class);
        acb.add(BgpAggregator.class);
        acb.add(AsPath.class);
        acb.add(ClusterId.class);
        acb.add(Community.class);
        acb.add(Communities.class);
        acb.add(ExtendedCommunity.class);
        acb.add(ExtendedCommunities.class);
        acb.add(LocalPref.class);
        acb.add(MultiExitDisc.class);
        acb.add(Origin.class);
        acb.add(OriginatorId.class);
        ATTRIBUTE_CACHEABLES = acb.build();
    }

    private final YangInstanceIdentifier tableId;
    private final RIBSupport tableSupport;
    private BindingNormalizedNodeCachingCodec<Attributes> attributeCodec;
    private final Object nlriCodec;

    TableContext(final RIBSupport tableSupport, final YangInstanceIdentifier tableId, final BindingCodecTreeFactory codecFactory) {
        this.tableSupport = Preconditions.checkNotNull(tableSupport);
        this.tableId = Preconditions.checkNotNull(tableId);

        final Builder<Class<? extends DataObject>> acb = ImmutableSet.builder();
        acb.addAll(ATTRIBUTE_CACHEABLES);
        acb.addAll(tableSupport.cacheableAttributeObjects());
        // FIXME: new Codec.create(acb.build(), tableSupport.cacheableNlriObjects());
        this.attributeCodec = null;
        // FIXME: new Codec.create(tableSupport.cacheableNlriObjects());
        this.nlriCodec = null;
        this.codecFactory = codecFactory;
    }

    YangInstanceIdentifier getTableId() {
        return this.tableId;
    }

    void onRuntimeContextUpdated(BindingRuntimeContext context) {
        BindingCodecTree codecTree = codecFactory.create(context);


        BindingCodecTreeNode<?> currentCodec = codecTree.getSubtreeCodec(tableId);
        NormalizedNodeContainer<?,?,?> emptySubtree = tableSupport.emptyRoutes();
        currentCodec = currentCodec.yangPathArgumentChild(emptySubtree.getIdentifier());
        // FIXME: Walk to first map node.
        while (!Route.class.isAssignableFrom(currentCodec.getBindingClass())) {
            emptySubtree = (NormalizedNodeContainer<?,?,?>) Iterables.getOnlyElement(emptySubtree.getValue());
            currentCodec.yangPathArgumentChild(emptySubtree.getIdentifier());
        }

        /*
         *  Binding codec tree allows resolution of children by binding class,
         *  where correct namespace is derived from parent node if child class
         *  was resolved using grouping.
         */
        BindingCodecTreeNode<Attributes> attrContext = currentCodec.streamChild(Attributes.class);
        attributeCodec = attrContext.createCachingCodec(tableSupport.cacheableAttributeObjects());
    }

    static void clearTable(final DOMDataWriteTransaction tx, final RIBSupport tableSupport, final YangInstanceIdentifier tableId) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> tb = ImmutableNodes.mapEntryBuilder();
        tb.withNodeIdentifier((NodeIdentifierWithPredicates)tableId.getLastPathArgument());
        tb.withChild(EMPTY_TABLE_ATTRIBUTES);

        // tableId is keyed, but that fact is not directly visible from YangInstanceIdentifier, see BUG-2796
        final NodeIdentifierWithPredicates tableKey = (NodeIdentifierWithPredicates) tableId.getLastPathArgument();
        for (final Entry<QName, Object> e : tableKey.getKeyValues().entrySet()) {
            tb.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        final ChoiceNode routes = tableSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", tableSupport);
        Verify.verify(Routes.QNAME.equals(routes.getNodeType()), "Empty routes have unexpected identifier %s, expected %s", routes.getNodeType(), Routes.QNAME);

        tx.put(LogicalDatastoreType.OPERATIONAL, tableId, tb.withChild(routes).build());
    }

    void clearTable(final DOMDataWriteTransaction tx) {
        clearTable(tx, this.tableSupport, this.tableId);
    }

    void removeTable(final DOMDataWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, this.tableId);
    }

    void writeRoutes(final Object codecFactory, final DOMDataWriteTransaction tx, final MpReachNlri nlri, final PathAttributes attributes) {

        // FIXME: run the decoder process
        final ContainerNode domNlri = (ContainerNode) this.nlriCodec;

        Attributes routeBaAttributes = new AttributesBuilder(attributes).build();
        // FIXME: run the decoder process
        final ContainerNode routeAttributes = (ContainerNode) attributeCodec.serialize(routeBaAttributes);

        this.tableSupport.putRoutes(tx, this.tableId, domNlri, routeAttributes);
    }

    void removeRoutes(final Object object, final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        // FIXME: run the decoder process
        final ContainerNode domNlri = (ContainerNode) this.nlriCodec;
        // FIXME : causes ApplicationPeerTest to fail, uncomment, when codecs are ready
       // this.tableSupport.deleteRoutes(tx, this.tableId, domNlri);
    }
}
