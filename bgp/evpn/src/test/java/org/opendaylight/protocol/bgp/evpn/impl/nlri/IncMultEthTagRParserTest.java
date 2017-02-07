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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IP_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETI;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ROUDE_DISTIN;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.WRONG_VALUE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.MACIpAdvRParserTest.createEti;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.ORI_NID;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.evpn.choice.IncMultiEthernetTagResCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.inc.multi.ethernet.tag.res.IncMultiEthernetTagResBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class IncMultEthTagRParserTest {
    static final byte[] RESULT = {
        (byte) 0x03, (byte) 0x11,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] VALUE = {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
        (byte) 0x20, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private IncMultEthTagRParser parser;

    @Before
    public void setUp() {
        this.parser = new IncMultEthTagRParser();
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void parserTest() {

        final IncMultiEthernetTagResCase expected = IncMultEthTagRParserTest.createIncMultiCase();
        assertArrayEquals(RESULT, ByteArray.getAllBytes(this.parser.serializeEvpn(expected, Unpooled.wrappedBuffer(ROUDE_DISTIN))));

        final EvpnChoice result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE));
        assertEquals(expected, result);

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(IncMultEthTagRParser.INC_MULT_ROUTE_NID);
        final ContainerNode incMult = createContBuilder(IncMultEthTagRParser.INC_MULT_ROUTE_NID).addChild(createEti())
            .addChild(createValueBuilder(IP_MODEL, ORI_NID).build()).build();
        final EvpnChoice modelResult = this.parser.serializeEvpnModel(incMult);
        assertEquals(expected, modelResult);

        final EvpnChoice keyResult = this.parser.createRouteKey(incMult);
        assertEquals(expected, keyResult);
    }

    public static IncMultiEthernetTagResCase createIncMultiCase() {
        return new IncMultiEthernetTagResCaseBuilder().setIncMultiEthernetTagRes(new IncMultiEthernetTagResBuilder().setEthernetTagId(ETI)
            .setOrigRouteIp(IP).build()).build();
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