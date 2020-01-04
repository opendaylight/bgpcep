/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import static org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity.INET_LOCAL_ADMIN_LENGTH;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommonBuilder;

public final class Inet4SpecificExtendedCommunityCommonUtil {
    private Inet4SpecificExtendedCommunityCommonUtil() {
        // Hidden on purpose
    }

    public static Inet4SpecificExtendedCommunityCommon parseCommon(final ByteBuf buffer) {
        return new Inet4SpecificExtendedCommunityCommonBuilder()
                .setGlobalAdministrator(Ipv4Util.addressForByteBuf(buffer))
                .setLocalAdministrator(ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH))
                .build();
    }

    public static void serializeCommon(final Inet4SpecificExtendedCommunityCommon extComm,
            final ByteBuf byteAggregator) {
        ByteBufWriteUtil.writeIpv4Address(extComm.getGlobalAdministrator(), byteAggregator);
        byteAggregator.writeBytes(extComm.getLocalAdministrator());
    }
}
