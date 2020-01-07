/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.VrfRouteImportExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.VrfRouteImportExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.vrf.route._import.extended.community._case.VrfRouteImportExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommonBuilder;

public class VrfRouteImportHandlerTest {

    private static final byte[] INPUT = {12, 51, 2, 5, 21, 45};
    private final VrfRouteImportHandler handler = new VrfRouteImportHandler();

    @Test
    public void testHandler() {
        final VrfRouteImportExtendedCommunityCase expected = new VrfRouteImportExtendedCommunityCaseBuilder()
                .setVrfRouteImportExtendedCommunity(new VrfRouteImportExtendedCommunityBuilder()
                        .setInet4SpecificExtendedCommunityCommon(new Inet4SpecificExtendedCommunityCommonBuilder()
                                .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                                .setLocalAdministrator(new byte[]{21, 45}).build())
                        .build())
                .build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());

        assertEquals(11, this.handler.getSubType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() {
        this.handler.serializeExtendedCommunity(new As4GenericSpecExtendedCommunityCaseBuilder().build(), null);
    }
}
