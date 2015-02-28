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
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

/**
 * A context for a single RIB table instance. It is always bound to a particular {@link AdjRibInWriter}.
 *
 * FIXME: need a better name once we local-rib and rib-out contexts
 */
@NotThreadSafe
final class TableContext {
    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(Attributes.QNAME);
    private static final ContainerNode EMPTY_ROUTE_ATTRIBUTES = ImmutableNodes.containerNode(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes.QNAME);
    private static final Set<Class<? extends DataObject>> ATTRIBUTE_CACHEABLES = ImmutableSet.of();
    private final YangInstanceIdentifier tableId;
    private final RIBSupport tableSupport;

    private final Object attributeCodec;
    private final Object nlriCodec;

    TableContext(final RIBSupport tableSupport, final YangInstanceIdentifier tableId) {
        this.tableSupport = Preconditions.checkNotNull(tableSupport);
        this.tableId = Preconditions.checkNotNull(tableId);

        final Builder<Class<? extends DataObject>> acb = ImmutableSet.builder();
        acb.addAll(ATTRIBUTE_CACHEABLES);
        acb.addAll(tableSupport.cacheableAttributeObjects());

        // FIXME: new Codec.create(acb.build(), tableSupport.cacheableNlriObjects());
        attributeCodec = null;

        // FIXME: new Codec.create(tableSupport.cacheableNlriObjects());
        nlriCodec = null;
    }

    YangInstanceIdentifier getTableId() {
        return tableId;
    }

    void clearTable(final DOMDataWriteTransaction tx) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> tb =
                ImmutableNodes.mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates)tableId.getLastPathArgument()).withChild(EMPTY_TABLE_ATTRIBUTES);

        final ChoiceNode routes = tableSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", this);
        Verify.verify(Routes.QNAME.equals(routes.getNodeType()), "Empty routes have unexpected identifier %s, expected %s", routes.getNodeType(), Routes.QNAME);

        tx.put(LogicalDatastoreType.OPERATIONAL, tableId, tb.withChild(routes).build());
    }

    void removeTable(final DOMDataWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, tableId);
    }

    void writeRoutes(final Object codecFactory, final DOMDataWriteTransaction tx, final MpReachNlri nlri, final PathAttributes attributes) {

        // FIXME: run the decoder process
        final ContainerNode domNlri = (ContainerNode) nlriCodec;

        // FIXME: run the decoder process
        final ContainerNode domAttributes = (ContainerNode) attributeCodec;
        final ContainerNode routeAttributes = Builders.containerBuilder(EMPTY_ROUTE_ATTRIBUTES).withValue(domAttributes.getValue()).build();

        tableSupport.putRoutes(tx, tableId, domNlri, routeAttributes);
    }

    void removeRoutes(final Object object, final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        // FIXME: run the decoder process
        final ContainerNode domNlri = (ContainerNode) nlriCodec;

        tableSupport.deleteRoutes(tx, tableId, domNlri);
    }
}
