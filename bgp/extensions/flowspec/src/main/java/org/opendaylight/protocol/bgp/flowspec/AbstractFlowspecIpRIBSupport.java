/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

abstract class AbstractFlowspecIpRIBSupport<
        T extends AbstractFlowspecIpNlriParser,
        C extends Routes & DataObject,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<?>> extends AbstractFlowspecRIBSupport<T, C, S, R> {
    AbstractFlowspecIpRIBSupport(final BindingNormalizedNodeSerializer mappingService, final Class<C> cazeClass,
            final Class<S> containerClass, final Class<R> listClass, final AddressFamily afiClass,
            final SubsequentAddressFamily safiClass, final QName dstContainerClassQName, final T nlriParser) {
        super(mappingService, cazeClass, containerClass, listClass, afiClass, safiClass, dstContainerClassQName,
            nlriParser);
    }

    @Override
    protected final DestinationType buildDestination(final MapEntryNode route, final PathId pathId) {
        return nlriParser.createAdvertizedRoutesDestinationType(nlriParser.extractFlowspec(route), pathId);
    }

    @Override
    protected final DestinationType buildWithdrawnDestination(final MapEntryNode route, final PathId pathId) {
        return nlriParser.createWithdrawnDestinationType(nlriParser.extractFlowspec(route), pathId);
    }
}
