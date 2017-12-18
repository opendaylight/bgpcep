/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IP;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IPV6;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IPV6_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IP_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MAC;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MAC_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL2;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL2_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VLAN;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest.LAN_AUT_GEN_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ROUDE_DISTIN;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.ETI_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.IP_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MAC_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS1_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS2_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.VLAN_NID;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.MacIpAdvRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.MacIpAdvRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.ip.adv.route.MacIpAdvRouteBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class MACIpAdvRParserTest {
    private static final byte[] VALUE = {
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02,
        (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
        (byte) 0x30, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x05, (byte) 0xdc, (byte) 0x20
    };
    private static final byte[] RESULT = {
        (byte) 0x02, (byte) 0x28,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02,
        (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
        (byte) 0x30, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x05, (byte) 0xdc, (byte) 0x20
    };
    private static final byte[] VALUE2 = {
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02,
        (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
        (byte) 0x30, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7,
        (byte) 0x80, 0x20, (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x01, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
    };
    private static final byte[] RESULT2 = {
        (byte) 0x02, (byte) 0x31,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,  //RD
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02,
        (byte) 0x02, (byte) 0x00, //ESI
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, // ETI
        (byte) 0x30, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, // MAC
        (byte) 0x80,//IPV6
        (byte) 0x20, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x05, (byte) 0xdc, (byte) 0x10, //MPLS
    };

    private MACIpAdvRParser parser;

    @Before
    public void setUp() {
        this.parser = new MACIpAdvRParser();
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void parserCase1Test() {
        final MacIpAdvRouteCase expected = createdExpectedResult();
        assertArrayEquals(RESULT, ByteArray.getAllBytes(this.parser.serializeEvpn(expected,
                Unpooled.wrappedBuffer(ROUDE_DISTIN))));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE));
        assertEquals(expected, result);
        final EvpnChoice modelResult = this.parser.serializeEvpnModel(createMacIpCont());
        assertEquals(expected, modelResult);

        final EvpnChoice keyResult = this.parser.createRouteKey(createMacIpCont());
        assertEquals(createdExpectedRouteKey(), keyResult);
    }

    static MacIpAdvRouteCase createdExpectedResult() {
        return new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(new MacIpAdvRouteBuilder().setEsi(LAN_AUT_GEN_CASE)
                .setEthernetTagId(ETI).setMacAddress(MAC).setIpAddress(IP).setMplsLabel1(MPLS_LABEL)
                .setMplsLabel2(MPLS_LABEL2).build()).build();
    }

    static MacIpAdvRouteCase createdExpectedRouteKey() {
        return new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(new MacIpAdvRouteBuilder().setEthernetTagId(ETI)
                .setMacAddress(MAC).setIpAddress(IP).build()).build();
    }

    static ContainerNode createMacIpCont() {
        return createContBuilder(MACIpAdvRParser.MAC_IP_ADV_ROUTE_NID)
            .addChild(LanParserTest.createLanChoice())
            .addChild(createEti())
            .addChild(createValueBuilder(MAC_MODEL, MAC_NID).build())
            .addChild(createValueBuilder(IP_MODEL, IP_NID).build())
            .addChild(createValueBuilder(MPLS_LABEL_MODEL, MPLS1_NID).build())
            .addChild(createValueBuilder(MPLS_LABEL2_MODEL, MPLS2_NID).build())
            .build();
    }

    public static ContainerNode createEti() {
        return createContBuilder(ETI_NID).withChild(createValueBuilder(VLAN, VLAN_NID).build()).build();
    }

    @Test
    public void parserCase2Test() {

        final MacIpAdvRouteCase expected = new MacIpAdvRouteCaseBuilder().setMacIpAdvRoute(new MacIpAdvRouteBuilder()
                .setEsi(LAN_AUT_GEN_CASE).setEthernetTagId(ETI).setMacAddress(MAC).setIpAddress(IPV6)
                .setMplsLabel1(MPLS_LABEL).build()).build();
        assertArrayEquals(RESULT2, ByteArray.getAllBytes(this.parser.serializeEvpn(expected,
                Unpooled.wrappedBuffer(ROUDE_DISTIN))));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE2));
        assertEquals(expected, result);

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(MACIpAdvRParser.MAC_IP_ADV_ROUTE_NID);
        final ContainerNode macIp = createContBuilder(MACIpAdvRParser.MAC_IP_ADV_ROUTE_NID)
                .addChild(LanParserTest.createLanChoice()).addChild(createEti())
            .addChild(createValueBuilder(MAC_MODEL, MAC_NID).build())
                .addChild(createValueBuilder(IPV6_MODEL, IP_NID).build())
            .addChild(createValueBuilder(MPLS_LABEL_MODEL, MPLS1_NID).build()).build();
        final EvpnChoice modelResult = this.parser.serializeEvpnModel(macIp);

        assertEquals(expected, modelResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEvpn(new EsRouteCaseBuilder().build(), null);
    }
}