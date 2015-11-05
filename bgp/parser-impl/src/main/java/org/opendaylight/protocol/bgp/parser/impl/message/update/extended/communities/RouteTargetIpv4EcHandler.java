/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.ipv4._case.RouteTargetIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.ipv4._case.RouteTargetIpv4Builder;

public final class RouteTargetIpv4EcHandler extends AbstractIpv4ExtendedCommunity {

    private static final int SUBTYPE = 2;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof RouteTargetIpv4Case,
                "The extended community %s is not RouteTargetAsTwoOctetCase type.", extendedCommunity);
        final RouteTargetIpv4 routeTarget = ((RouteTargetIpv4Case) extendedCommunity).getRouteTargetIpv4();
        ByteBufWriteUtil.writeIpv4Address(routeTarget.getGlobalAdministrator(), byteAggregator);
        ByteBufWriteUtil.writeUnsignedShort(routeTarget.getLocalAdministrator(), byteAggregator);
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final RouteTargetIpv4 routeTarget = new RouteTargetIpv4Builder()
            .setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer))
            .setLocalAdministrator(buffer.readUnsignedShort())
            .build();
        return new RouteTargetIpv4CaseBuilder().setRouteTargetIpv4(routeTarget).build();
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
