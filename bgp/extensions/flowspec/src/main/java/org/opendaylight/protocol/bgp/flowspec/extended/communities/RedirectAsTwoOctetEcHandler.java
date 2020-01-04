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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.extended.community.RedirectExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.redirect.extended.community.RedirectExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev180329.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class RedirectAsTwoOctetEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 128;
    private static final int SUBTYPE = 8;
    private static final int TRAFFIC_RATE_SIZE = 4;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.RedirectExtendedCommunity,
                "The extended community %s is not RedirectExtendedCommunityCase type.", extendedCommunity);
        final RedirectExtendedCommunity redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bgp.flowspec.rev180329.RedirectExtendedCommunity) extendedCommunity).getRedirectExtendedCommunity();
        ByteBufWriteUtil.writeUnsignedShort(redirect.getGlobalAdministrator().getValue().intValue(), byteAggregator);
        byteAggregator.writeBytes(redirect.getLocalAdministrator());
    }

    @Override
    public int getType(final boolean isTransitive) {
        //redirect is transitive only
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        final ShortAsNumber as1 = new ShortAsNumber(Uint32.valueOf(ByteBufUtils.readUint16(buffer)));
        final byte[] byteValue = ByteArray.readBytes(buffer, TRAFFIC_RATE_SIZE);
        return new RedirectExtendedCommunityCaseBuilder()
            .setRedirectExtendedCommunity(new RedirectExtendedCommunityBuilder()
                .setGlobalAdministrator(as1)
                .setLocalAdministrator(byteValue)
                .build())
            .build();
    }
}
