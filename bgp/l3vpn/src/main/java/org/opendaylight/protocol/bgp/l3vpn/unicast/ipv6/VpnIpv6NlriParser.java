/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6;

import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.unicast.AbstractVpnNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.L3vpnIpv6Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.destination.VpnIpv6DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;

public class VpnIpv6NlriParser extends AbstractVpnNlriParser {

    private static <T extends L3vpnIpv6Destination> List<VpnDestination> getVpnDestination(final DestinationType dst,
            final Class<T> dstTypeCaseClazz) {
        return dstTypeCaseClazz.isInstance(dst) ? dstTypeCaseClazz.cast(dst).getVpnIpv6Destination().getVpnDestination()
                : null;
    }

    @Override
    protected List<VpnDestination> getWithdrawnVpnDestination(final DestinationType dstType) {
        return getVpnDestination(dstType,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update.attributes
                    .mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6Case.class);
    }

    @Override
    protected List<VpnDestination> getAdvertizedVpnDestination(final DestinationType dstType) {
        return getVpnDestination(
            dstType,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update.attributes
                    .mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6Case.class);
    }

    @Override
    protected WithdrawnRoutes getWithdrawnRoutesByDestination(final List<VpnDestination> dst) {
        return new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update
                    .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                    .setVpnIpv6Destination(new VpnIpv6DestinationBuilder()
                            .setVpnDestination(dst).build()).build()).build();
    }

    @Override
    protected AdvertizedRoutes getAdvertizedRoutesByDestination(final List<VpnDestination> dst) {
        return new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update
                    .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                    .setVpnIpv6Destination(new VpnIpv6DestinationBuilder()
                            .setVpnDestination(dst).build()).build()).build();
    }

}
