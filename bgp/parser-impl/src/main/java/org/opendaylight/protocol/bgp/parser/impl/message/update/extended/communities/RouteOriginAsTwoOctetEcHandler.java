/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractTwoOctetAsExtendedCommunity;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;

public final class RouteOriginAsTwoOctetEcHandler extends AbstractTwoOctetAsExtendedCommunity {

    private static final int SUBTYPE = 3;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final RouteOriginExtendedCommunity targetOrigin = new RouteOriginExtendedCommunityBuilder()
            .setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
            .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH))
            .build();
        return new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(targetOrigin).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof RouteOriginExtendedCommunityCase,
                "The extended community %s is not RouteOriginExtendedCommunity type.", extendedCommunity);
        final RouteOriginExtendedCommunity routeOrigin = ((RouteOriginExtendedCommunityCase) extendedCommunity).getRouteOriginExtendedCommunity();
        ByteBufWriteUtil.writeUnsignedShort(Ints.checkedCast(routeOrigin.getGlobalAdministrator().getValue()), byteAggregator);
        byteAggregator.writeBytes(routeOrigin.getLocalAdministrator());
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
