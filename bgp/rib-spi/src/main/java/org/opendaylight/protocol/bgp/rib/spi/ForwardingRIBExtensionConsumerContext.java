/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

abstract class ForwardingRIBExtensionConsumerContext implements RIBExtensionConsumerContext {
    @Override
    public final <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>>
            RIBSupport<C, S> getRIBSupport(final TablesKey key) {
        return delegate().getRIBSupport(key);
    }

    @Override
    public final <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>>
            RIBSupport<C, S> getRIBSupport(final AddressFamily afi, final SubsequentAddressFamily safi) {
        return delegate().getRIBSupport(afi, safi);
    }

    @Override
    public final <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>>
            RIBSupport<C, S> getRIBSupport(final NodeIdentifierWithPredicates key) {
        return delegate().getRIBSupport(key);
    }

    abstract @NonNull RIBExtensionProviderContext delegate();
}
