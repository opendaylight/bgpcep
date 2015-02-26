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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

/**
 * A context for a single RIB table instance. It is always bound to a particular {@link AdjRibInWriter}.
 *
 * FIXME: need a better name once we local-rib and rib-out contexts
 */
final class TableContext {
    private static final ContainerNode EMPTY_ATTRIBUTES = ImmutableNodes.containerNode(Attributes.QNAME);
    private final YangInstanceIdentifier tableId;
    private final RIBSupport tableSupport;

    TableContext(final RIBSupport tableSupport, final YangInstanceIdentifier tableId) {
        this.tableSupport = Preconditions.checkNotNull(tableSupport);
        this.tableId = Preconditions.checkNotNull(tableId);
    }

    protected final YangInstanceIdentifier getTableId() {
        return tableId;
    }

    final void clearTable(final DOMDataWriteTransaction tx) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> tb =
                ImmutableNodes.mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates)tableId.getLastPathArgument()).withChild(EMPTY_ATTRIBUTES);

        final ChoiceNode routes = tableSupport.emptyRoutes();
        Verify.verifyNotNull(routes, "Null empty routes in %s", this);
        Verify.verify(Routes.QNAME.equals(routes.getNodeType()), "Empty routes have unexpected identifier %s, expected %s", routes.getNodeType(), Routes.QNAME);

        tx.put(LogicalDatastoreType.OPERATIONAL, tableId, tb.withChild(routes).build());
    }

    final void removeTable(final DOMDataWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, tableId);
    }
}
