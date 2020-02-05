/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.FourOctAsCommonECUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;

/**
 * Route Target 4 Octet As Handler.
 *
 * @author Claudio D. Gasparini
 */
public final class As4RouteTargetExtendedHandler {
    private As4RouteTargetExtendedHandler() {
        // Hidden on purpose
    }

    public static As4RouteTargetExtendedCommunity parse(final ByteBuf body) {
        return new As4RouteTargetExtendedCommunityBuilder()
                .setAs4SpecificCommon(FourOctAsCommonECUtil.parseCommon(body)).build();
    }

    public static void serialize(final As4RouteTargetExtendedCommunity as4RouteTargetExtendedCommunity,
            final ByteBuf body) {
        FourOctAsCommonECUtil.serializeCommon(as4RouteTargetExtendedCommunity.getAs4SpecificCommon(), body);
    }
}
