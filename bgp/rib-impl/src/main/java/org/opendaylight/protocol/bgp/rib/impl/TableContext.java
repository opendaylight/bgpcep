/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * A context for a single RIB table instance. It is always bound to a particular {@link AdjRibInWriter}.
 *
 * FIXME: need a better name once we local-rib and rib-out contexts
 */
@NotThreadSafe
final class TableContext {
    private final YangInstanceIdentifier tableId;
    private final RIBSupportContext tableSupport;

    TableContext(final RIBSupportContext tableSupport, final YangInstanceIdentifier tableId) {
        this.tableSupport = requireNonNull(tableSupport);
        this.tableId = requireNonNull(tableId);
    }

    YangInstanceIdentifier getTableId() {
        return this.tableId;
    }


    void createEmptyTableStructure(final DOMDataWriteTransaction tx) {
        this.tableSupport.createEmptyTableStructure(tx, this.tableId);
    }

    void removeTable(final DOMDataWriteTransaction tx) {
        tx.delete(LogicalDatastoreType.OPERATIONAL, this.tableId);
    }

    Set<NodeIdentifierWithPredicates> writeRoutes(final DOMDataWriteTransaction tx, final MpReachNlri nlri,
                                                  final Attributes attributes) {
        return this.tableSupport.writeRoutes(tx, this.tableId, nlri, attributes);
    }

    void removeRoutes(final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        this.tableSupport.deleteRoutes(tx, this.tableId, nlri);
    }

    <C extends Routes & DataObject & ChoiceIn<Tables>,
            S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>,
            I extends Identifier<R>>
    RIBSupport<C, S, R, I> getRibSupport() {
        return tableSupport.getRibSupport();
    }
}
