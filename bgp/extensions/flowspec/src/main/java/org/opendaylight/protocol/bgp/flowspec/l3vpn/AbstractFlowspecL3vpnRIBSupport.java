/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public abstract class AbstractFlowspecL3vpnRIBSupport
        <T extends AbstractFlowspecL3vpnNlriParser,
                C extends Routes & DataObject,
                S extends ChildOf<? super C>,
                R extends Route & ChildOf<? super S> & Identifiable<?>>
        extends AbstractFlowspecRIBSupport<T, C, S, R> {

    protected AbstractFlowspecL3vpnRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<R> listClass,
            final QName dstContainerClassQName,
            final AddressFamily afiClass,
            final T flowspecNlriParser
    ) {
        super(mappingService, cazeClass, containerClass, listClass, afiClass,
            FlowspecL3vpnSubsequentAddressFamily.VALUE, dstContainerClassQName, flowspecNlriParser);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        final RouteDistinguisher rd = extractRouteDistinguisher(routesCont);
        return this.nlriParser.createAdvertizedRoutesDestinationType(
                new Object[]{rd, this.nlriParser.extractFlowspec(routesCont)},
                pathId
        );
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        final RouteDistinguisher rd = extractRouteDistinguisher(routesCont);
        return this.nlriParser.createWithdrawnDestinationType(
                new Object[]{rd, this.nlriParser.extractFlowspec(Iterables.getOnlyElement(routes))},
                pathId
        );
    }
}
