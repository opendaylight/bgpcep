/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.destination.VpnDestination;

/**
 * @author Kevin Wang
 */
public class VpnIpv6NlriParser extends AbstractVpnNlriParser {

    @Override
    protected List<VpnDestination> getWithdrawnVpnDestination(DestinationType dstType) {
        return getVpnDestination(
            dstType,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6Case.class
        );
    }

    @Override
    protected List<VpnDestination> getAdvertizedVpnDestination(DestinationType dstType) {
        return getVpnDestination(
            dstType,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6Case.class
        );
    }

    @Override
    protected WithdrawnRoutes getWithdrawnRoutesByDestination(List<VpnDestination> dst) {
        return new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnDestination(dst).build()
        ).build();
    }

    @Override
    protected AdvertizedRoutes getAdvertizedRoutesByDestination(List<VpnDestination> dst) {
        return new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnDestination(dst).build()
        ).build();
    }
}
