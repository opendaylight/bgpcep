/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * A context for a single RIB table instance. It is always bound to a particular {@link AdjRibInWriter}.
 *
 * <p>This class is NOT thread-safe.
 */
// FIXME: need a better name once we local-rib and rib-out contexts
final class TableContext {
    private final @NonNull YangInstanceIdentifier tableId;
    private final @NonNull RIBSupportContext tableSupport;

    TableContext(final RIBSupportContext tableSupport, final YangInstanceIdentifier tableId) {
        this.tableSupport = requireNonNull(tableSupport);
        this.tableId = requireNonNull(tableId);
    }

    @NonNull YangInstanceIdentifier getTableId() {
        return tableId;
    }

    void createEmptyTableStructure(final DOMDataTreeWriteTransaction tx) {
        tableSupport.createEmptyTableStructure(tx, tableId);
    }

    void removeTable(final DOMDataTreeWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, tableId);
    }

    List<NodeIdentifierWithPredicates> writeRoutes(final DOMDataTreeWriteTransaction tx, final MpReachNlri nlri,
            final Attributes attributes) {
        return tableSupport.writeRoutes(tx, tableId, nlri, attributes);
    }

    void removeRoutes(final DOMDataTreeWriteTransaction tx, final MpUnreachNlri nlri) {
        tableSupport.deleteRoutes(tx, tableId, nlri);
    }

    YangInstanceIdentifier routesPath() {
        return tableSupport.getRibSupport().routesPath(tableId);
    }

    YangInstanceIdentifier routePath(final PathArgument routeId) {
        return tableSupport.getRibSupport().routePath(tableId, routeId);
    }
}
