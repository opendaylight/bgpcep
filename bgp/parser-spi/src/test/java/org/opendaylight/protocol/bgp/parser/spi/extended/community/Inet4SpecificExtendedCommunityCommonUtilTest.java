/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommonBuilder;

public class Inet4SpecificExtendedCommunityCommonUtilTest {
    private static final byte[] INPUT = {
        12, 51, 2, 5, 21, 45
    };

    @Test
    public void testHandle() {
        final Inet4SpecificExtendedCommunityCommon expected = new Inet4SpecificExtendedCommunityCommonBuilder()
                .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                .setLocalAdministrator(new byte[]{21, 45}).build();

        final Inet4SpecificExtendedCommunityCommon exComm = Inet4SpecificExtendedCommunityCommonUtil
                .parseCommon(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        Inet4SpecificExtendedCommunityCommonUtil.serializeCommon(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }
}
