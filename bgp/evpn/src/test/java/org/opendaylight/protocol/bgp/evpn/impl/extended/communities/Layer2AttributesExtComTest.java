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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NormalizationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.OperationalMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.Layer2AttributesExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.Layer2AttributesExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.layer._2.attributes.extended.community.Layer2AttributesExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class Layer2AttributesExtComTest {
    private static final byte[] EXPECTEDS = {(byte) 0x00, (byte) 0x07, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x00};
    private static final byte[] EVPN_VPWS = {(byte) 0x00, (byte) 0x2f, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x00};
    private static final byte[] EVPN_VPWS_2 = {(byte) 0x00, (byte) 0x57, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x00};
    private Layer2AttributesExtCom parser;

    @Before
    public void setUp() {
        this.parser = new Layer2AttributesExtCom();
    }

    @Test
    public void handlerTest() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf buff = Unpooled.buffer(COMMUNITY_VALUE_SIZE);

        final Layer2AttributesExtendedCommunityCase expected = new Layer2AttributesExtendedCommunityCaseBuilder()
                .setLayer2AttributesExtendedCommunity(new Layer2AttributesExtendedCommunityBuilder().setBackupPe(true)
                        .setControlWord(true).setPrimaryPe(true).setL2Mtu(257).build()).build();
        this.parser.serializeExtendedCommunity(expected, buff);
        final byte[] resultByte = ByteArray.getAllBytes(buff);
        assertArrayEquals(EXPECTEDS, resultByte);

        final ExtendedCommunity result = this.parser.parseExtendedCommunity(Unpooled.wrappedBuffer(EXPECTEDS));
        assertEquals(expected, result);
    }

    @Test
    public void evpnVpwsTest() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf buff = Unpooled.buffer(COMMUNITY_VALUE_SIZE);

        final Layer2AttributesExtendedCommunityCase expected = new Layer2AttributesExtendedCommunityCaseBuilder()
                .setLayer2AttributesExtendedCommunity(new Layer2AttributesExtendedCommunityBuilder().setBackupPe(true)
                        .setControlWord(true).setPrimaryPe(true).setL2Mtu(257)
                        .setModeOfOperation(OperationalMode.VlanAwareFxc)
                        .setOperatingPer(NormalizationType.SingleVid)
                        .build()).build();
        this.parser.serializeExtendedCommunity(expected, buff);
        final byte[] resultByte = ByteArray.getAllBytes(buff);
        assertArrayEquals(EVPN_VPWS, resultByte);

        final ExtendedCommunity result = this.parser.parseExtendedCommunity(Unpooled.wrappedBuffer(EVPN_VPWS));
        assertEquals(expected, result);

        final ByteBuf buff2 = Unpooled.buffer(COMMUNITY_VALUE_SIZE);
        final Layer2AttributesExtendedCommunityCase expected2 = new Layer2AttributesExtendedCommunityCaseBuilder()
                .setLayer2AttributesExtendedCommunity(new Layer2AttributesExtendedCommunityBuilder().setBackupPe(true)
                        .setControlWord(true).setPrimaryPe(true).setL2Mtu(257)
                        .setModeOfOperation(OperationalMode.VlanUnawareFxc)
                        .setOperatingPer(NormalizationType.DoubleVid)
                        .build()).build();
        this.parser.serializeExtendedCommunity(expected2, buff2);
        final byte[] resultByte2 = ByteArray.getAllBytes(buff2);
        assertArrayEquals(EVPN_VPWS_2, resultByte2);

        final ExtendedCommunity result2 = this.parser.parseExtendedCommunity(Unpooled.wrappedBuffer(EVPN_VPWS_2));
        assertEquals(expected2, result2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeExtendedCommunity(new DefaultGatewayExtendedCommunityCaseBuilder().build(), null);
    }

    @Test
    public void testSubtype() {
        assertEquals(4, this.parser.getSubType());
    }
}