/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.ModelUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.ModelUtil.createValueBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.router.id.generated._case.RouterIdGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class RouterIdParserTest extends AbstractParserTest {
    public static final byte[] RESULT = {(byte) 0x04, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x02, (byte) 0x02, (byte) 0x02,
        (byte) 0x02, (byte) 0x00};
    private static final byte[] VALUE = {(byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x02, (byte) 0x02, (byte) 0x02,
        (byte) 0x02, (byte) 0x00};
    private static final Ipv4Address ROUTE_ID = new Ipv4Address("42.42.42.42");
    public static final RouterIdGeneratedCase ROUTE_ID_CASE = new RouterIdGeneratedCaseBuilder().setRouterIdGenerated(new RouterIdGeneratedBuilder()
        .setLocalDiscriminator(LD).setRouterId(ROUTE_ID).build()).build();
    private RouterIdParser parser;

    @Before
    public void setUp() {
        this.parser = new RouterIdParser();
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = Unpooled.buffer(VALUE_SIZE);

        this.parser.serializeEsi(ROUTE_ID_CASE, buff);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final Esi acResult = this.parser.parseEsi(Unpooled.wrappedBuffer(VALUE));
        assertEquals(ROUTE_ID_CASE, acResult);

        final Esi acmResult = this.parser.serializeEsi(RouterIdParserTest.createRouteContainer());
        assertEquals(ROUTE_ID_CASE, acmResult);
    }

    public static ChoiceNode createRouterIdCase() {
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> nextHop = Builders.choiceBuilder();
        nextHop.withNodeIdentifier(RouterIdParser.ROUTER_ID_CASE_NID);
        return nextHop.addChild(createRouteContainer()).build();
    }

    private static ContainerNode createRouteContainer() {
        return createContBuilder(RouterIdParser.ROUTER_ID_NID)
            .addChild(createValueBuilder(ROUTE_ID, RouterIdParser.RD_NID).build())
            .addChild(createValueBuilder(LD, AbstractEsiType.LD_NID).build())
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEsi(new ArbitraryCaseBuilder().build(), null);
    }
}