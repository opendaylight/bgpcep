/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn.mcast.nlri;

import static org.opendaylight.protocol.util.Ipv6Util.IPV6_LENGTH;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;

public final class L3vpnMcastNlriSerializer {
    private L3vpnMcastNlriSerializer() {
        throw new UnsupportedOperationException();
    }

    static List<L3vpnMcastDestination> extractDest(final ByteBuf nlri, final boolean addPathSupported) {
        List<L3vpnMcastDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final L3vpnMcastDestinationBuilder builder = new L3vpnMcastDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final int length = nlri.readUnsignedByte();
            builder.setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(nlri));
            if (length == IPV6_LENGTH) {
                builder.setPrefix(new IpPrefix(Ipv6Util.prefixForByteBuf(nlri)));
            } else {
                builder.setPrefix(new IpPrefix(Ipv4Util.prefixForByteBuf(nlri)));
            }
            dests.add(builder.build());
        }
        return dests;
    }

    public static void serializeNlri(final List<L3vpnMcastDestination> destinationList, final ByteBuf output) {
        ByteBuf nlriOutput = Unpooled.buffer();
        for (final L3vpnMcastDestination dest : destinationList) {
            final ByteBuf nlriCommon = Unpooled.buffer();
            RouteDistinguisherUtil.serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriCommon);
            final IpPrefix prefix = dest.getPrefix();
            if (prefix.getIpv4Prefix() != null) {
                nlriCommon.writeBytes(Ipv4Util.bytesForPrefix(prefix.getIpv4Prefix()));
            } else {
                nlriCommon.writeBytes(Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()));
            }
            nlriOutput.writeBytes(nlriCommon);
        }
        output.writeBytes(nlriOutput);
    }
}
