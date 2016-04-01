/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.l3vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestination;

public class VpnIpv4NlriParser extends AbstractVpnNlriParser {

    @Override
    protected List<VpnDestination> getWithdrawnVpnDestination(DestinationType dstType) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.VpnIpv4DestinationCase) dstType)
            .getVpnIpv4Destination().getVpnDestination();
    }

    @Override
    protected List<VpnDestination> getAdvertizedVpnDestination(DestinationType dstType) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.VpnIpv4DestinationCase) dstType)
            .getVpnIpv4Destination().getVpnDestination();
    }

    @Override
    protected WithdrawnRoutes getWithdrawnRoutesByDestination(List<VpnDestination> dst) {
        return new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.VpnIpv4DestinationCaseBuilder().setVpnIpv4Destination(
                new VpnIpv4DestinationBuilder().setVpnDestination(dst).build()
            ).build()
        ).build();
    }

    @Override
    protected AdvertizedRoutes getAdvertizedRoutesByDestination(List<VpnDestination> dst) {
        return new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160210.update.attributes.mp.reach.nlri.advertized.routes.destination.type.VpnIpv4DestinationCaseBuilder().setVpnIpv4Destination(
                new VpnIpv4DestinationBuilder().setVpnDestination(dst).build()
            ).build()
        ).build();
    }
}
