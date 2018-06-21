/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.flowspec.extended.communities;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.ip.nh.extended.community.RedirectIpNhExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.ip.nh.extended.community.RedirectIpNhExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.extended.communities.extended.community.RedirectIpNhExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class RedirectIpNextHopEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    //https://tools.ietf.org/html/draft-ietf-idr-flowspec-redirect-ip-00#section-7
    private static final int TYPE = 8;

    private static final int SUBTYPE = 0;

    private static final byte COPY = 0x1;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectIpNhExtendedCommunity,
                "The extended community %s is not RedirectIpNhExtendedCommunityCase type.", extendedCommunity);
        final RedirectIpNhExtendedCommunity redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectIpNhExtendedCommunity) extendedCommunity).getRedirectIpNhExtendedCommunity();
        final IpAddress nextHopAddress = redirect.getNextHopAddress();
        if (nextHopAddress.getIpv4Address() != null) {
            ByteBufWriteUtil.writeIpv4Address(nextHopAddress.getIpv4Address(), byteAggregator);
        } else {
            ByteBufWriteUtil.writeIpv6Address(nextHopAddress.getIpv6Address(), byteAggregator);
        }
        ByteBufWriteUtil.writeUnsignedShort((redirect.isCopy() == null || !redirect.isCopy()) ? 0 : 1, byteAggregator);
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final RedirectIpNhExtendedCommunityBuilder builder = new RedirectIpNhExtendedCommunityBuilder();
        if (buffer.readableBytes() > Ipv6Util.IPV6_LENGTH ) {
            builder.setNextHopAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer)));
        } else {
            builder.setNextHopAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)));
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
