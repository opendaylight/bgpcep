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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest.LAN_AUT_GEN_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ROUDE_DISTIN;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.WRONG_VALUE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.ORI_NID;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.es.route.EsRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EsRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.evpn.choice.MacIpAdvRouteCaseBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class EthSegRParserTest {
    private static final byte[] VALUE = {
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] RESULT = {
        (byte) 0x04, (byte) 0x17,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] VALUE2 = {
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x80,//IPV6
        (byte) 0x20, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
    };
    private static final byte[] RESULT2 = {
        (byte) 0x04, (byte) 0x23,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x80,//IPV6
        (byte) 0x20, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
    };
    private EthSegRParser parser;

    @Before
    public void setUp() {
        this.parser = new EthSegRParser();
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void parserTest() {
        final EsRouteCase expected = new EsRouteCaseBuilder().setEsRoute(new EsRouteBuilder().setEsi(LAN_AUT_GEN_CASE).setOrigRouteIp(IP).build()).build();
        assertArrayEquals(RESULT, ByteArray.getAllBytes(this.parser.serializeEvpn(expected, Unpooled.wrappedBuffer(ROUDE_DISTIN))));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE));
        assertEquals(expected, result);

        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(EthSegRParser.ES_ROUTE_NID);
        final ContainerNode arbitraryC = createContBuilder(EthSegRParser.ES_ROUTE_NID).addChild(LanParserTest.createLanChoice())
            .addChild(createValueBuilder(IP_MODEL, ORI_NID).build()).build();
        final EvpnChoice modelResult = this.parser.serializeEvpnModel(arbitraryC);
        assertEquals(expected, modelResult);

        final EvpnChoice keyResult = this.parser.createRouteKey(arbitraryC);
        assertEquals(expected, keyResult);
    }

    @Test
    public void parser2Test() {
        final EsRouteCase expected = new EsRouteCaseBuilder().setEsRoute(new EsRouteBuilder().setEsi(LAN_AUT_GEN_CASE).setOrigRouteIp(IPV6).build()).build();
        assertArrayEquals(RESULT2, ByteArray.getAllBytes(this.parser.serializeEvpn(expected, Unpooled.wrappedBuffer(ROUDE_DISTIN))));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE2));
        assertEquals(expected, result);

        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(EthSegRParser.ES_ROUTE_NID);
        final ContainerNode arbitraryC = createContBuilder(EthSegRParser.ES_ROUTE_NID).addChild(LanParserTest.createLanChoice())
            .addChild(createValueBuilder(IPV6_MODEL, ORI_NID).build()).build();
        final EvpnChoice modelResult = this.parser.serializeEvpnModel(arbitraryC);
        assertEquals(expected, modelResult);

        final EvpnChoice keyResult = this.parser.createRouteKey(arbitraryC);
        assertEquals(expected, keyResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEvpn(new MacIpAdvRouteCaseBuilder().build(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongSizeTest() {
        this.parser.parseEvpn(Unpooled.wrappedBuffer(WRONG_VALUE));
    }
}