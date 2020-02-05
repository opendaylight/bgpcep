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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.redirect.as4.extended.community.RedirectAs4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.redirect.as4.extended.community.RedirectAs4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.extended.communities.extended.community.RedirectAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class RedirectAsFourOctetEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 130;
    private static final int SUBTYPE = 8;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.RedirectAs4ExtendedCommunity,
                "The extended community %s is not RedirectAs4ExtendedCommunityCase type.", extendedCommunity);
        final RedirectAs4 redirect = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec
                .rev180329.RedirectAs4ExtendedCommunity) extendedCommunity).getRedirectAs4();
        ByteBufUtils.write(byteAggregator, redirect.getGlobalAdministrator().getValue());
        ByteBufUtils.writeOrZero(byteAggregator, redirect.getLocalAdministrator());
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        return new RedirectAs4ExtendedCommunityCaseBuilder()
                .setRedirectAs4(new RedirectAs4Builder()
                    .setGlobalAdministrator(new AsNumber(ByteBufUtils.readUint32(buffer)))
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
