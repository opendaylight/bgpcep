/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnRIBSupport;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.destination.DestinationVpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.destination.DestinationVpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.destination.destination.vpn.ipv4.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.destination.destination.vpn.ipv4.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.routes.vpn.ipv4.routes.VpnRoute;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

final class VpnIpv4RIBSupport extends AbstractVpnRIBSupport<VpnDestination> {
    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     */
    public VpnIpv4RIBSupport() {
        super(VpnIpv4RoutesCase.class, VpnIpv4Routes.class, VpnRoute.class, Ipv4AddressFamily.class, DestinationVpnIpv4.QNAME, VpnDestination.QNAME);
    }

    @Override
    protected VpnDestination extractVpnDestination(DataContainerNode<? extends PathArgument> route) {
        final VpnDestination dst = new VpnDestinationBuilder()
            .setPrefix(LabeledUnicastRIBSupport.extractPrefix(route, PREFIX_TYPE_NID))
            .setLabelStack(LabeledUnicastRIBSupport.extractLabel(route, LABEL_STACK_NID, LV_NID))
            .setRouteDistinguisher(extractRouteDistinguisher(route))
            .build();
        return dst;
    }

    @Override
    protected DestinationType getAdvertizedDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
            new DestinationVpnIpv4Builder().setVpnDestination(dests).build()
        ).build();
    }

    @Override
    protected DestinationType getWithdrawnDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder().setDestinationVpnIpv4(
            new DestinationVpnIpv4Builder().setVpnDestination(dests).build()
        ).build();
    }
}
