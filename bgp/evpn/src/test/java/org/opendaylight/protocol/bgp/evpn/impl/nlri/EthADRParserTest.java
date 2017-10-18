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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VLAN;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest.LAN_AUT_GEN_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.MACIpAdvRParserTest.createEti;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.MPLS_NID;
import static org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistryTest.EVPN_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.ethernet.a.d.route.EthernetADRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EthernetADRouteCaseBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class EthADRParserTest {
    public static final byte[] RESULT = {
        (byte) 0x01, (byte) 0x19,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x05, (byte) 0xdc, (byte) 0x10
    };
    public static final byte[] ROUDE_DISTIN = {
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02
    };
    static final EthernetTagId ETI = new EthernetTagIdBuilder().setVlanId(VLAN).build();
    public static final EthernetADRouteCase ETHERNET_AD_ROUTE_CASE_KEY = new EthernetADRouteCaseBuilder()
        .setEthernetADRoute(new EthernetADRouteBuilder().setEsi(LAN_AUT_GEN_CASE).setEthernetTagId(ETI).build()).build();
    public static final EthernetADRouteCase ETHERNET_AD_ROUTE_CASE = new EthernetADRouteCaseBuilder().setEthernetADRoute(new EthernetADRouteBuilder()
        .setEsi(LAN_AUT_GEN_CASE).setEthernetTagId(ETI).setMplsLabel(MPLS_LABEL).build()).build();
    static final byte[] WRONG_VALUE = {(byte) 0x00};
    private static final byte[] VALUE = {
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x05, (byte) 0xdc, (byte) 0x10
    };
    private EthADRParser parser;

    @Before
    public void setUp() {
        this.parser = new EthADRParser();
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = this.parser.serializeEvpn(ETHERNET_AD_ROUTE_CASE, Unpooled.wrappedBuffer(ROUDE_DISTIN));
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE));
        assertEquals(ETHERNET_AD_ROUTE_CASE, result);

        final EvpnChoice modelResult = this.parser.serializeEvpnModel(EthADRParserTest.createArbitraryContainer());
        assertEquals(ETHERNET_AD_ROUTE_CASE, modelResult);

        final EvpnChoice keyResult = this.parser.createRouteKey(EthADRParserTest.createArbitraryContainer());
        assertEquals(ETHERNET_AD_ROUTE_CASE_KEY, keyResult);
    }

    public static ChoiceNode createEthADRModel() {
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(EVPN_NID);
        return choice.addChild(createArbitraryContainer()).build();
    }

    private static ContainerNode createArbitraryContainer() {
        final ChoiceNode esiModel = LanParserTest.createLanChoice();
        return createContBuilder(EthADRParser.ETH_AD_ROUTE_NID).addChild(esiModel).addChild(createEti())
            .addChild(createValueBuilder(MPLS_LABEL_MODEL, MPLS_NID).build()).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEvpn(new EsRouteCaseBuilder().build(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongSizeTest() {
        this.parser.parseEvpn(Unpooled.wrappedBuffer(WRONG_VALUE));
    }
}