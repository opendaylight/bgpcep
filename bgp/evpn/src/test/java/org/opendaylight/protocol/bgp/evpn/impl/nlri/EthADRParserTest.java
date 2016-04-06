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
import static org.opendaylight.protocol.bgp.evpn.impl.ModelUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.ModelUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest.LAN_AUT_GEN_CASE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.LanParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EsRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.EthernetADRouteCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class EthADRParserTest {
    private static final byte[] VALUE = {
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x05, (byte) 0xdc, (byte) 0x10
    };
    private static final byte[] RESULT = {
        (byte) 0x01, (byte) 0x19,
        (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02,
        (byte) 0x02, (byte) 0xf2, (byte) 0x0c, (byte) 0xdd, (byte) 0x80, (byte) 0x9f, (byte) 0xf7, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x05, (byte) 0xdc, (byte) 0x10
    };
    private static final MplsLabel MPLS_LABEL = new MplsLabel(24001L);
    private static final RouteDistinguisher RD = RouteDistinguisherBuilder.getDefaultInstance("1.2.3.4:258");
    private static final EthernetTagId ETI = new EthernetTagIdBuilder().setVlanId(10L).build();
    private EthADRParser parser;

    @Before
    public void setUp() {
        this.parser = new EthADRParser();
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = Unpooled.buffer(EthADRParser.CONTENT_LENGTH + 2);

        final EthernetADRouteCase acb = new EthernetADRouteCaseBuilder()
            .setRouteDistinguisher(RD)
            .setEsi(LAN_AUT_GEN_CASE)
            .setEthernetTagId(ETI)
            .setMplsLabel(MPLS_LABEL)
            .build();
        this.parser.serializeEvpn(acb, buff);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final Evpn result = this.parser.parseEvpn(Unpooled.wrappedBuffer(VALUE));
        assertEquals(acb, result);

        final ChoiceNode esiModel = LanParserTest.createLanChoice();

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> nextHop = Builders.choiceBuilder();
        nextHop.withNodeIdentifier(EthADRParser.ETH_AD_ROUTE_CASE_NID);
        final ContainerNode arbitraryC = createContBuilder(EthADRParser.ETH_AD_ROUTE_NID)
            .addChild(createValueBuilder(RD, EthADRParser.RD_NID).build())
            .addChild(createValueBuilder(esiModel, EthADRParser.ESI_NID).build())
            .addChild(createValueBuilder(ETI, EthADRParser.ETI_NID).build())
            .addChild(createValueBuilder(MPLS_LABEL, EthADRParser.MPLS_NID).build())
            .build();
        final ChoiceNode resultNextHop = nextHop.addChild(arbitraryC).build();
        final Evpn modelResult = this.parser.serializeEvpnModel(resultNextHop);
        assertEquals(acb, modelResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEvpn(new EsRouteCaseBuilder().build(), null);
    }
}