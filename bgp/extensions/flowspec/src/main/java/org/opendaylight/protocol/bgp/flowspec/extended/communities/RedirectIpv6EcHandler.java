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
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.RedirectIpv6ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.redirect.ipv6.extended.community.RedirectIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.redirect.ipv6.extended.community.RedirectIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.extended.communities.extended.community.RedirectIpv6ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class RedirectIpv6EcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 128;
    private static final int SUBTYPE = 11;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof RedirectIpv6ExtendedCommunity,
            "The extended community %s is not RedirectIpv6ExtendedCommunity type.", extendedCommunity);
        final RedirectIpv6 redirectIpv6 = ((RedirectIpv6ExtendedCommunity) extendedCommunity).getRedirectIpv6();
        byteAggregator.writeBytes(Ipv6Util.byteBufForAddress(redirectIpv6.getGlobalAdministrator()));
        ByteBufUtils.writeOrZero(byteAggregator, redirectIpv6.getLocalAdministrator());
    }

    @Override
    public int getType(final boolean isTransitive) {
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        return new RedirectIpv6ExtendedCommunityCaseBuilder()
            .setRedirectIpv6(new RedirectIpv6Builder()
                .setGlobalAdministrator(Ipv6Util.addressForByteBuf(buffer))
                .setLocalAdministrator(ByteBufUtils.readUint16(buffer))
                .build())
            .build();
    }
}
