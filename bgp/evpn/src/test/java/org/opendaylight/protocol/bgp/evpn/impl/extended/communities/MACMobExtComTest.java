/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.extended.communities;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.COMMUNITY_VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.LD;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.mobility.extended.community.MacMobilityExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class MACMobExtComTest {
    private static final byte[] RESULT = {(byte) 0x01, (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02};
    private MACMobExtCom parser;

    @Before
    public void setUp() {
        this.parser = new MACMobExtCom();
    }

    @Test
    public void parserTest() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf buff = Unpooled.buffer(COMMUNITY_VALUE_SIZE);

        final MacMobilityExtendedCommunityCase expected = new MacMobilityExtendedCommunityCaseBuilder()
                .setMacMobilityExtendedCommunity(new MacMobilityExtendedCommunityBuilder()
                        .setStatic(true).setSeqNumber(LD).build()).build();
        this.parser.serializeExtendedCommunity(expected, buff);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final ExtendedCommunity result = this.parser.parseExtendedCommunity(Unpooled.wrappedBuffer(RESULT));
        assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeExtendedCommunity(new DefaultGatewayExtendedCommunityCaseBuilder().build(), null);
    }

    @Test
    public void testSubtype() {
        assertEquals(0, this.parser.getSubType());
    }
}