/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn;

import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.extractRouteDistinguisher;

import com.google.common.collect.Iterables;
import java.util.Collection;
import javax.annotation.Nonnull;

import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public abstract class AbstractFlowspecL3vpnRIBSupport<T extends AbstractFlowspecL3vpnNlriParser> extends AbstractFlowspecRIBSupport<T> {
    private final NodeIdentifier routeDistinguisherNID;

    protected AbstractFlowspecL3vpnRIBSupport(
        final Class<? extends Routes> cazeClass,
        final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass,
        final QName dstContainerClassQName,
        final Class<? extends AddressFamily> afiClass,
        final T flowspecNlriParser
    ) {
        super(cazeClass, containerClass, listClass, afiClass, FlowspecL3vpnSubsequentAddressFamily.class, dstContainerClassQName, flowspecNlriParser);
        this.routeDistinguisherNID = new NodeIdentifier(QName.create(routeQName(), "route-distinguisher").intern());
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
        final RouteDistinguisher rd = extractRouteDistinguisher(routesCont, this.routeDistinguisherNID);
        return this.nlriParser.createAdvertizedRoutesDestinationType(
            new Object[] {rd, this.nlriParser.extractFlowspec(routesCont)},
            pathId
        );
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        final MapEntryNode routesCont = Iterables.getOnlyElement(routes);
        final PathId pathId = PathIdUtil.buildPathId(routesCont, routePathIdNid());
            final RouteDistinguisher rd = extractRouteDistinguisher(routesCont, this.routeDistinguisherNID);
        return this.nlriParser.createWithdrawnDestinationType(
            new Object[] {rd, this.nlriParser.extractFlowspec(Iterables.getOnlyElement(routes))},
            pathId
        );
    }
}
