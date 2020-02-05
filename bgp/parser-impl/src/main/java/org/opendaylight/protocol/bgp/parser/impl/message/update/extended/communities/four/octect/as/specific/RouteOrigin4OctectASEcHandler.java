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
import org.opendaylight.protocol.bgp.parser.spi.extended.community.Abstract4OctetAsExtendedCommunity;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.FourOctAsCommonECUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;

public final class RouteOrigin4OctectASEcHandler extends Abstract4OctetAsExtendedCommunity {
    private static final int SUBTYPE = 3;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf body) {
        return new As4RouteOriginExtendedCommunityCaseBuilder()
                .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                        .setAs4SpecificCommon(FourOctAsCommonECUtil.parseCommon(body)).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof As4RouteOriginExtendedCommunityCase,
                "The extended community %s is not As4RouteOriginExtendedCommunityCase type.",
                extendedCommunity);
        FourOctAsCommonECUtil.serializeCommon(((As4RouteOriginExtendedCommunityCase) extendedCommunity)
                .getAs4RouteOriginExtendedCommunity().getAs4SpecificCommon(), body);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
