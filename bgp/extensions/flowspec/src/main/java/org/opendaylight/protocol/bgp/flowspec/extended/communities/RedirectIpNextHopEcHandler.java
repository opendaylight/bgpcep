/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.extended.communities;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.ip.nh.extended.community.RedirectIpNhExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.ip.nh.extended.community.RedirectIpNhExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;

public class RedirectIpNextHopEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    //https://tools.ietf.org/html/draft-ietf-idr-flowspec-redirect-ip-00#section-7
    private static final int TYPE = 8;
    private static final int SUBTYPE = 0;
    private static final byte COPY = 0x1;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.RedirectIpNhExtendedCommunity,
                "The extended community %s is not RedirectIpNhExtendedCommunityCase type.", extendedCommunity);
        final RedirectIpNhExtendedCommunity redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.flowspec.rev180329.RedirectIpNhExtendedCommunity) extendedCommunity)
                .getRedirectIpNhExtendedCommunity();
        final IpAddressNoZone nextHopAddress = redirect.getNextHopAddress();
        if (nextHopAddress.getIpv4AddressNoZone() != null) {
            Ipv4Util.writeIpv4Address(nextHopAddress.getIpv4AddressNoZone(), byteAggregator);
        } else {
            Ipv6Util.writeIpv6Address(nextHopAddress.getIpv6AddressNoZone(), byteAggregator);
        }
        byteAggregator.writeShort(Boolean.TRUE.equals(redirect.isCopy()) ? 1 : 0);
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        final RedirectIpNhExtendedCommunityBuilder builder = new RedirectIpNhExtendedCommunityBuilder();
        if (buffer.readableBytes() > Ipv6Util.IPV6_LENGTH) {
            builder.setNextHopAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer)));
        } else {
            builder.setNextHopAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer)));
        }
        builder.setCopy((buffer.readUnsignedShort() & COPY) == 1);
        return new RedirectIpNhExtendedCommunityCaseBuilder().setRedirectIpNhExtendedCommunity(builder.build()).build();
    }

    @Override
    public int getType(final boolean isTransitive) {
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
