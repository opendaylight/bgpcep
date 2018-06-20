/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import static org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractTwoOctetAsExtendedCommunity.AS_LOCAL_ADMIN_LENGTH;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.extended.community.grouping.RouteTargetExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.route.target.extended.community.grouping.RouteTargetExtendedCommunityBuilder;

/**
 * Route Target Extended Community Parser / Serializer.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetExtendedCommunityHandler {
    private RouteTargetExtendedCommunityHandler() {
        throw new UnsupportedOperationException();
    }

    public static RouteTargetExtendedCommunity parse(final ByteBuf buffer) {
        return new RouteTargetExtendedCommunityBuilder()
                .setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
                .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH))
                .build();
    }

    public static void serialize(final RouteTargetExtendedCommunity routeTarget, final ByteBuf byteAggregator) {
        ByteBufWriteUtil.writeUnsignedShort(Ints.checkedCast(routeTarget.getGlobalAdministrator().getValue()),
                byteAggregator);
        byteAggregator.writeBytes(routeTarget.getLocalAdministrator());
    }
}
