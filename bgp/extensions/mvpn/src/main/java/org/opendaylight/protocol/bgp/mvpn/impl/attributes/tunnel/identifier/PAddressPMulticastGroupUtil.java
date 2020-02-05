/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.PAddressPMulticastGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.bidir.pim.tree.BidirPimTreeBuilder;

final class PAddressPMulticastGroupUtil {
    private PAddressPMulticastGroupUtil() {
        // Hidden on purpose
    }

    static void serializeIpAddress(final IpAddressNoZone ipAddress, final ByteBuf byteBuf) {
        if (ipAddress.getIpv4AddressNoZone() != null) {
            byteBuf.writeBytes(Ipv4Util.bytesForAddress(ipAddress.getIpv4AddressNoZone()));
        } else {
            byteBuf.writeBytes(Ipv6Util.bytesForAddress(ipAddress.getIpv6AddressNoZone()));
        }
    }

    static IpAddressNoZone parseIpAddress(final int ipLength, final ByteBuf buffer) {
        if (ipLength == Ipv6Util.IPV6_LENGTH) {
            return new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_LENGTH) {
            return new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer));
        }
        return null;
    }

    static void serializeSenderPMulticastGroup(final PAddressPMulticastGroup bidir, final ByteBuf byteBuf) {
        serializeIpAddress(bidir.getPAddress(), byteBuf);
        serializeIpAddress(bidir.getPMulticastGroup(), byteBuf);
    }

    static PAddressPMulticastGroup parseSenderPMulticastGroup(final ByteBuf buffer) {
        final int ipLength = buffer.readableBytes() / 2;
        final IpAddressNoZone pSenderAddress = parseIpAddress(ipLength, buffer);
        final IpAddressNoZone pMulticastGroup = parseIpAddress(ipLength, buffer);
        return new BidirPimTreeBuilder().setPAddress(pSenderAddress).setPMulticastGroup(pMulticastGroup).build();
    }
}
