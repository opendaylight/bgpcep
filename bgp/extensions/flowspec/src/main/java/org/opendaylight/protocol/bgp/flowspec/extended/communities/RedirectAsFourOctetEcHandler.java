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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.as4.extended.community.RedirectAs4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.redirect.as4.extended.community.RedirectAs4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public final class RedirectAsFourOctetEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 130;

    private static final int SUBTYPE = 8;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectAs4ExtendedCommunity,
                "The extended community %s is not RedirectAs4ExtendedCommunityCase type.", extendedCommunity);
        final RedirectAs4 redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.RedirectAs4ExtendedCommunity) extendedCommunity).getRedirectAs4();
        ByteBufWriteUtil.writeUnsignedInt(redirect.getGlobalAdministrator().getValue(), byteAggregator);
        ByteBufWriteUtil.writeUnsignedShort(redirect.getLocalAdministrator(), byteAggregator);
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final RedirectAs4Builder builder = new RedirectAs4Builder();
        builder.setGlobalAdministrator(new AsNumber(buffer.readUnsignedInt()));
        builder.setLocalAdministrator(buffer.readUnsignedShort());
        return new RedirectAs4ExtendedCommunityCaseBuilder().setRedirectAs4(builder.build()).build();
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
