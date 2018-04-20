/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.Abstract4OctetAsExtendedCommunity;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.FourOctAsCommonECUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.As4RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.As4RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.as._4.route.target.extended.community._case.As4RouteTargetExtendedCommunityBuilder;

public final class RouteTarget4OctectASEcHandler extends Abstract4OctetAsExtendedCommunity {
    private static final int SUBTYPE = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf body) throws BGPDocumentedException, BGPParsingException {
        return new As4RouteTargetExtendedCommunityCaseBuilder().setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
            .setAs4SpecificCommon(FourOctAsCommonECUtil.parseCommon(body)).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof As4RouteTargetExtendedCommunityCase,
            "The extended community %s is not As4RouteTargetExtendedCommunityCase type.", extendedCommunity);
        FourOctAsCommonECUtil.serializeCommon(((As4RouteTargetExtendedCommunityCase) extendedCommunity).getAs4RouteTargetExtendedCommunity()
            .getAs4SpecificCommon(), body);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
