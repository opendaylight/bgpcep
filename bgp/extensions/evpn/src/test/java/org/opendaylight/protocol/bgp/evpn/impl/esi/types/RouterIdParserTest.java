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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.LD;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValue;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.LD_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.RD_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.RouterIdGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.RouterIdGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.router.id.generated._case.RouterIdGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.router.id.generated._case.RouterIdGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class RouterIdParserTest {
    public static final byte[] RESULT = {(byte) 0x04, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x02,
        (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x00};
    private static final byte[] VALUE = {(byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x2A, (byte) 0x02, (byte) 0x02,
        (byte) 0x02, (byte) 0x02, (byte) 0x00};
    private static final Ipv4AddressNoZone ROUTE_ID = new Ipv4AddressNoZone("42.42.42.42");
    public static final RouterIdGeneratedCase ROUTE_ID_CASE = new RouterIdGeneratedCaseBuilder()
            .setRouterIdGenerated(new RouterIdGeneratedBuilder()
                    .setLocalDiscriminator(LD).setRouterId(ROUTE_ID).build()).build();
    private static final String ROUTE_ID_MODEL = "42.42.42.42";
    private RouterIdParser parser;

    @Before
    public void setUp() {
        parser = new RouterIdParser();
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = Unpooled.buffer(VALUE_SIZE);

        parser.serializeEsi(ROUTE_ID_CASE, buff);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final Esi acResult = parser.parseEsi(Unpooled.wrappedBuffer(VALUE));
        assertEquals(ROUTE_ID_CASE, acResult);

        final Esi acmResult = parser.serializeEsi(RouterIdParserTest.createRouteContainer());
        assertEquals(ROUTE_ID_CASE, acmResult);
    }

    public static ChoiceNode createRouterIdCase() {
        return Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(RouterIdGeneratedCase.QNAME))
            .addChild(createRouteContainer())
            .build();
    }

    private static ContainerNode createRouteContainer() {
        return createContBuilder(new NodeIdentifier(RouterIdGenerated.QNAME))
            .addChild(createValue(ROUTE_ID_MODEL, RD_NID))
            .addChild(createValue(LD, LD_NID))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        parser.serializeEsi(new ArbitraryCaseBuilder().build(), null);
    }
}