/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.OpaqueUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.MulticastGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.CGAddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.CGAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.LdpMpOpaqueValueCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.LdpMpOpaqueValueCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.c.g.address._case.CGAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.ldp.mp.opaque.value._case.LdpMpOpaqueValueBuilder;

/**
 * Multicast Group  with Opaque values Utility.
 *
 * @author Claudio D. Gasparini
 */
final class MulticastGroupOpaqueUtil {
    private MulticastGroupOpaqueUtil() {
        throw new UnsupportedOperationException();
    }

    static MulticastGroup multicastGroupForByteBuf(final ByteBuf buffer) {
        final short multicastGroupLength = buffer.readUnsignedByte();
        if (multicastGroupLength == Ipv4Util.IP4_BITS_LENGTH) {
            return new CGAddressCaseBuilder().setCGAddress(new CGAddressBuilder()
                    .setCGAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)))
                    .build()).build();
        } else if (multicastGroupLength == Ipv6Util.IPV6_BITS_LENGTH) {
            return new CGAddressCaseBuilder().setCGAddress(new CGAddressBuilder()
                    .setCGAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build()).build();
        } else {
            return new LdpMpOpaqueValueCaseBuilder()
                    .setLdpMpOpaqueValue(new LdpMpOpaqueValueBuilder(OpaqueUtil.parseOpaque(buffer)).build()).build();
        }
    }

    static void bytesForMulticastGroup(final MulticastGroup group, final ByteBuf nlriByteBuf) {
        if (group instanceof CGAddressCase) {
            nlriByteBuf.writeBytes(IpAddressUtil.bytesFor(((CGAddressCase) group).getCGAddress().getCGAddress()));
        } else {
            OpaqueUtil.serializeOpaque(((LdpMpOpaqueValueCase) group).getLdpMpOpaqueValue(), nlriByteBuf);
        }
    }
}
