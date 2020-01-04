/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.ipv4.grouping.RouteTargetIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.ipv4.grouping.RouteTargetIpv4Builder;

/**
 * Route Target Ipv4 Handler.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetIpv4Handler {
    private RouteTargetIpv4Handler() {
        throw new UnsupportedOperationException();
    }

    public static void serialize(final RouteTargetIpv4 routeTarget, final ByteBuf byteAggregator) {
        ByteBufWriteUtil.writeIpv4Address(routeTarget.getGlobalAdministrator(), byteAggregator);
        ByteBufWriteUtil.writeUnsignedShort(routeTarget.getLocalAdministrator(), byteAggregator);
    }

    public static RouteTargetIpv4 parse(final ByteBuf buffer) {
        return new RouteTargetIpv4Builder()
                .setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer))
                .setLocalAdministrator(ByteBufUtils.readUint16(buffer))
                .build();
    }
}
