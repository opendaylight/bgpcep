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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.AS_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.AS_NUMBER;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.LD;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.AS_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.LD_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.AsGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.AsGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.as.generated._case.AsGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.as.generated._case.AsGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class ASGenParserTest {
    private static final byte[] VALUE = {(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02,
        (byte) 0x02, (byte) 0x00};
    private static final byte[] RESULT = {(byte) 0x05, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02,
        (byte) 0x02, (byte) 0x00};
    private ASGenParser parser;

    @Before
    public void setUp() {
        this.parser = new ASGenParser();
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = Unpooled.buffer(VALUE_SIZE);

        final AsGeneratedCase asGen = new AsGeneratedCaseBuilder().setAsGenerated(new AsGeneratedBuilder().setAs(AS_NUMBER).setLocalDiscriminator(LD)
            .build()).build();
        this.parser.serializeEsi(asGen, buff);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buff));

        final Esi acResult = this.parser.parseEsi(Unpooled.wrappedBuffer(VALUE));
        assertEquals(asGen, acResult);

        final ContainerNode cont = createContBuilder(new NodeIdentifier(AsGenerated.QNAME)).addChild(createValueBuilder(AS_MODEL, AS_NID).build())
            .addChild(createValueBuilder(LD, LD_NID).build()).build();
        final Esi acmResult = this.parser.serializeEsi(cont);
        assertEquals(asGen, acmResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEsi(new ArbitraryCaseBuilder().build(), null);
    }
}