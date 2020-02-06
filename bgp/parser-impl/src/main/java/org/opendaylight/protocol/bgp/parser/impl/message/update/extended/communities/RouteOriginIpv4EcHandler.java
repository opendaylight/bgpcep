/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteOriginIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.route.origin.ipv4._case.RouteOriginIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.route.origin.ipv4._case.RouteOriginIpv4Builder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class RouteOriginIpv4EcHandler extends AbstractIpv4ExtendedCommunity {
    private static final int SUBTYPE = 3;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof RouteOriginIpv4Case,
                "The extended community %s is not RouteOriginIpv4Case type.", extendedCommunity);
        final RouteOriginIpv4 routeTarget = ((RouteOriginIpv4Case) extendedCommunity).getRouteOriginIpv4();
        Ipv4Util.writeIpv4Address(routeTarget.getGlobalAdministrator(), byteAggregator);
        ByteBufUtils.writeOrZero(byteAggregator, routeTarget.getLocalAdministrator());
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        return new RouteOriginIpv4CaseBuilder()
                .setRouteOriginIpv4(new RouteOriginIpv4Builder()
                    .setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer))
                    .setLocalAdministrator(ByteBufUtils.readUint16(buffer))
                    .build())
                .build();
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
