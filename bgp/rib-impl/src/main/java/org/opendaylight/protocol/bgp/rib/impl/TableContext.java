/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

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

    void writeRoutes(final DOMDataWriteTransaction tx, final MpReachNlri nlri, final Attributes attributes) {
        this.tableSupport.writeRoutes(tx, this.tableId, nlri, attributes);
    }

    void removeRoutes(final DOMDataWriteTransaction tx, final MpUnreachNlri nlri) {
        this.tableSupport.deleteRoutes(tx, this.tableId, nlri);
    }
}
