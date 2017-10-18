/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 * {@link RIBSupport} wrapper which provides additional functionality
 * such as logic to update / remove routes using Binding DTOs
 * for BGP messages.
 *
 */
public abstract class RIBSupportContext {

    /**
     *
     * Create specified Rib table structure using supplied transaction.
     *
     * @param tx Transaction to to be used
     * @param tableId Instance Identifier of table to be cleared.
     */
    public abstract void createEmptyTableStructure(DOMDataWriteTransaction tx, YangInstanceIdentifier tableId);

    /**
     * Removes supplied routes from RIB table using supplied transaction.
     *
     * @param tx Transaction to be used
     * @param tableId Instance Identifier of table to be updated
     * @param nlri UnreachNlri which contains routes to be removed.
     */
    public abstract void deleteRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tableId, MpUnreachNlri nlri);

    /**
     *
     * Writes supplied routes and attributes to RIB table using supplied transaction.
     *
     *
     * @param tx Transaction to be used
     * @param tableId Instance Identifier of table to be updated
     * @param nlri ReachNlri which contains routes to be written.
     * @param attributes Attributes which should be written.
     */
    public abstract void writeRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tableId, MpReachNlri nlri,
            Attributes attributes);

    /**
     * Returns backing RIB support.
     *
     * @return RIBSupport
     */
    public abstract RIBSupport getRibSupport();
}
