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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.ipv4.extended.community.RedirectIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.ipv4.extended.community.RedirectIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.extended.communities.extended.community.RedirectIpv4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class RedirectIpv4EcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 129;

    private static final int SUBTYPE = 8;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectIpv4ExtendedCommunity,
                "The extended community %s is not RedirectIpv4ExtendedCommunityCase type.", extendedCommunity);
        final RedirectIpv4 redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectIpv4ExtendedCommunity) extendedCommunity).getRedirectIpv4();
        ByteBufWriteUtil.writeIpv4Address(redirect.getGlobalAdministrator(), byteAggregator);
        ByteBufWriteUtil.writeUnsignedShort(redirect.getLocalAdministrator(), byteAggregator);
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final RedirectIpv4Builder builder = new RedirectIpv4Builder();
        builder.setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer));
        builder.setLocalAdministrator(buffer.readUnsignedShort());
        return new RedirectIpv4ExtendedCommunityCaseBuilder().setRedirectIpv4(builder.build()).build();
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
