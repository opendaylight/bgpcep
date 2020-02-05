/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.Collection;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
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
 * {@link RIBSupport} wrapper which provides additional functionality
 * such as logic to update / remove routes using Binding DTOs
 * for BGP messages.
 */
public abstract class RIBSupportContext {
    /**
     * Create specified Rib table structure using supplied transaction.
     *
     * @param tx Transaction to to be used
     * @param tableId Instance Identifier of table to be cleared.
     */
    public abstract void createEmptyTableStructure(DOMDataTreeWriteTransaction tx, YangInstanceIdentifier tableId);

    /**
     * Removes supplied routes from RIB table using supplied transaction.
     *
     * @param tx Transaction to be used
     * @param tableId Instance Identifier of table to be updated
     * @param nlri UnreachNlri which contains routes to be removed.
     */
    public abstract void deleteRoutes(DOMDataTreeWriteTransaction tx, YangInstanceIdentifier tableId,
            MpUnreachNlri nlri);

    /**
     * Writes supplied routes and attributes to RIB table using supplied transaction.
     *
     * @param tx Transaction to be used
     * @param tableId Instance Identifier of table to be updated
     * @param nlri ReachNlri which contains routes to be written.
     * @param attributes Attributes which should be written.
     * @return Set of processed route key identifiers
     */
    public abstract Collection<NodeIdentifierWithPredicates> writeRoutes(DOMDataTreeWriteTransaction tx,
                                                                         YangInstanceIdentifier tableId,
                                                                         MpReachNlri nlri,
                                                                         Attributes attributes);

    /**
     * Returns backing RIB support.
     *
     * @return RIBSupport
     */
    public abstract <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
            RIBSupport<C, S, R, I> getRibSupport();
}
