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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.PAddressPMulticastGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.bidir.pim.tree.BidirPimTreeBuilder;

final class PAddressPMulticastGroupUtil {
    private PAddressPMulticastGroupUtil() {
        // Hidden on purpose
    }

    static void serializeIpAddress(final IpAddress ipAddress, final ByteBuf byteBuf) {
        if (ipAddress.getIpv4Address() != null) {
            byteBuf.writeBytes(Ipv4Util.bytesForAddress(ipAddress.getIpv4Address()));
        } else {
            byteBuf.writeBytes(Ipv6Util.bytesForAddress(ipAddress.getIpv6Address()));
        }
    }

    static IpAddress parseIpAddress(final int ipLength, final ByteBuf buffer) {
        if (ipLength == Ipv6Util.IPV6_LENGTH) {
            return new IpAddress(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_LENGTH) {
            return new IpAddress(Ipv4Util.addressForByteBuf(buffer));
        }
        return null;
    }

    static void serializeSenderPMulticastGroup(final PAddressPMulticastGroup bidir, final ByteBuf byteBuf) {
        serializeIpAddress(bidir.getPAddress(), byteBuf);
        serializeIpAddress(bidir.getPMulticastGroup(), byteBuf);
    }

    static PAddressPMulticastGroup parseSenderPMulticastGroup(final ByteBuf buffer) {
        final int ipLength = buffer.readableBytes() / 2;
        final IpAddress pSenderAddress = parseIpAddress(ipLength, buffer);
        final IpAddress pMulticastGroup = parseIpAddress(ipLength, buffer);
        return new BidirPimTreeBuilder().setPAddress(pSenderAddress).setPMulticastGroup(pMulticastGroup).build();
    }
}
