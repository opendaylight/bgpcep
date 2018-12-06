/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public interface RIBSupportContextRegistry {
    /**
     * Acquire a RIB Support for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is
     * not implemented.
     */
    @Nullable
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> RIBSupport<C, S, R, I> getRIBSupport(TablesKey key);

    /**
     * Acquire a RIB Support Context for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is
     * not implemented.
     */
    @Nullable
    RIBSupportContext getRIBSupportContext(TablesKey key);

    /**
     * Acquire a RIB Support Context for a AFI/SAFI combination.
     *
     * @param key Tables key with AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is
     * not implemented.
     */
    @Nullable
    RIBSupportContext getRIBSupportContext(NodeIdentifierWithPredicates key);
}
