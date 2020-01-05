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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.ipv4.extended.community.RedirectIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.ipv4.extended.community.RedirectIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class RedirectIpv4EcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 129;
    private static final int SUBTYPE = 8;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.RedirectIpv4ExtendedCommunity,
                "The extended community %s is not RedirectIpv4ExtendedCommunityCase type.", extendedCommunity);
        final RedirectIpv4 redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec
                .rev180329.RedirectIpv4ExtendedCommunity) extendedCommunity).getRedirectIpv4();
        ByteBufWriteUtil.writeIpv4Address(redirect.getGlobalAdministrator(), byteAggregator);
        ByteBufUtils.writeOrZero(byteAggregator, redirect.getLocalAdministrator());
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        return new RedirectIpv4ExtendedCommunityCaseBuilder()
                .setRedirectIpv4(new RedirectIpv4Builder()
                    .setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer))
                    .setLocalAdministrator(ByteBufUtils.readUint16(buffer))
                    .build())
                .build();
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
