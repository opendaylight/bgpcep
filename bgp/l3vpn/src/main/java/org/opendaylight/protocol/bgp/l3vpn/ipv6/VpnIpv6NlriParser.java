/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnNlriParser;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.l3vpn.ipv6.destination.DestinationVpnIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.l3vpn.ipv6.destination.destination.vpn.ipv6.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.l3vpn.ipv6.destination.destination.vpn.ipv6.VpnDestinationBuilder;

/**
 * @author Kevin Wang
 */
public class VpnIpv6NlriParser extends AbstractVpnNlriParser<VpnDestination> {

    @Override
    protected List<VpnDestination> getWithdrawnVpnDestination(DestinationType dstType) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6Case) dstType)
            .getDestinationVpnIpv6().getVpnDestination();
    }

    @Override
    protected List<VpnDestination> getAdvertizedVpnDestination(DestinationType dstType) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6Case) dstType)
            .getDestinationVpnIpv6().getVpnDestination();
    }

    @Override
    protected WithdrawnRoutes getWithdrawnRoutesByDestination(List<VpnDestination> dst) {
        return new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder().setDestinationVpnIpv6(
                new DestinationVpnIpv6Builder().setVpnDestination(dst).build()
            ).build()
        ).build();
    }

    @Override
    protected AdvertizedRoutes getAdvertizedRoutesByDestination(List<VpnDestination> dst) {
        return new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev160331.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder().setDestinationVpnIpv6(
                new DestinationVpnIpv6Builder().setVpnDestination(dst).build()
            ).build()
        ).build();
    }

    @Override
    protected List<VpnDestination> parseNlri(ByteBuf nlri, Class<? extends AddressFamily> afi) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<VpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final VpnDestinationBuilder builder = new VpnDestinationBuilder();
            final short length = nlri.readUnsignedByte();
            builder.setLabelStack(LUNlriParser.parseLabel(nlri));
            final int labelNum = builder.getLabelStack().size();
            final int prefixLen = length - (LUNlriParser.LABEL_LENGTH * Byte.SIZE * labelNum) - (RouteDistinguisherUtil.RD_LENGTH * Byte.SIZE);
            builder.setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(nlri));
            Preconditions.checkState(prefixLen > 0, "A valid VPN IP prefix is required.");
            builder.setPrefix(LUNlriParser.parseIpPrefix(nlri, prefixLen, afi));
            dests.add(builder.build());
        }
        return dests;
    }
}
