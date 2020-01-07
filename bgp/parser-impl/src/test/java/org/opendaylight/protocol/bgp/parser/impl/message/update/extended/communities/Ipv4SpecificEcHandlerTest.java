/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommonBuilder;

public class Ipv4SpecificEcHandlerTest {

    private static final byte[] INPUT = {
        12, 51, 2, 5, 21, 45
    };

    @Test
    public void testHandlerDeprecated() {
        final Ipv4SpecificEcHandler handler = new Ipv4SpecificEcHandler();
        final Inet4SpecificExtendedCommunityCase input
                = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder()
                        .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                        .setLocalAdministrator(new byte[]{21, 45}).build()).build();

        final Inet4SpecificExtendedCommunityCase expected
                = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder()
                        .setInet4SpecificExtendedCommunityCommon(new Inet4SpecificExtendedCommunityCommonBuilder()
                                .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                                .setLocalAdministrator(new byte[]{21, 45}).build()).build())
                .build();
        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeExtendedCommunity(input, output);
        assertArrayEquals(INPUT, output.array());
    }

    @Test
    public void testHandle() {
        final Ipv4SpecificEcHandler handler = new Ipv4SpecificEcHandler();
        final Inet4SpecificExtendedCommunityCase expected
                = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder()
                        .setInet4SpecificExtendedCommunityCommon(new Inet4SpecificExtendedCommunityCommonBuilder()
                                .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                                .setLocalAdministrator(new byte[]{21, 45}).build()).build())
                .build();

        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeExtendedCommunity(expected, output);
        assertArrayEquals(INPUT, output.array());
    }

}
