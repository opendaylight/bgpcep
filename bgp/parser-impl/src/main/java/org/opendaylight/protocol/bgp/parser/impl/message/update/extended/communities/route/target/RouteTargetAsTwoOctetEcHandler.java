/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractTwoOctetAsExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.extended.community.grouping.RouteTargetExtendedCommunity;

public final class RouteTargetAsTwoOctetEcHandler extends AbstractTwoOctetAsExtendedCommunity {

    private static final int SUBTYPE = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        return new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                RouteTargetExtendedCommunityHandler.parse(buffer)).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof RouteTargetExtendedCommunityCase,
                "The extended community %s is not RouteTargetExtendedCommunityCase type.",
                extendedCommunity);
        final RouteTargetExtendedCommunity routeTarget = ((RouteTargetExtendedCommunityCase) extendedCommunity)
                .getRouteTargetExtendedCommunity();
        RouteTargetExtendedCommunityHandler.serialize(routeTarget, byteAggregator);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

}
