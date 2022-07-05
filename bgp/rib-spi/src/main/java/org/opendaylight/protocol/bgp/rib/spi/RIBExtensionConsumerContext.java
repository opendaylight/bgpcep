/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Interface for acquiring AdjRIBsIn factories. In order for a model-driven RIB implementation to work correctly, it
 * has to know how to handle individual NLRI fields, whose encoding is specific to a AFI/SAFI pair. This interface
 * exposes an entry point for locating the AFI/SAFI-specific implementation handler.
 */
public interface RIBExtensionConsumerContext {

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is not implemented.
     */
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> @Nullable RIBSupport<C, S> getRIBSupport(
        @NonNull TablesKey key);

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param afi  Address Family Identifier
     * @param safi Subsequent Address Family identifier
     * @return RIBSupport instance, or null if the AFI/SAFI is not implemented.
     */
    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> @Nullable RIBSupport<C, S> getRIBSupport(
            @NonNull AddressFamily afi, @NonNull SubsequentAddressFamily safi);

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param key Tables key with AFI/SAFI
     * @return RIBSupport instance, or null if the AFI/SAFI is not implemented.
     */

    <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> @Nullable RIBSupport<C, S> getRIBSupport(
            @NonNull NodeIdentifierWithPredicates key);
}
