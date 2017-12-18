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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.ARB_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.ArbitraryCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.ArbitraryCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.AsGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.arbitrary._case.Arbitrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.arbitrary._case.ArbitraryBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class ArbitraryParserTest {
    private static final byte[] ARB_VALUE = {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05,
        (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09};
    private static final byte[] ARB_RESULT = {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
        (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09};
    private ArbitraryParser parser;

    @Before
    public void setUp() {
        this.parser = new ArbitraryParser();
    }

    @Test
    public void parserTest() {
        final ByteBuf buff = Unpooled.buffer(VALUE_SIZE);

        final ArbitraryCase arbitrary = new ArbitraryCaseBuilder().setArbitrary(new ArbitraryBuilder()
                .setArbitrary(ARB_VALUE).build()).build();
        this.parser.serializeEsi(arbitrary, buff);
        assertArrayEquals(ARB_RESULT, ByteArray.getAllBytes(buff));

        final Esi acResult = this.parser.parseEsi(Unpooled.wrappedBuffer(ARB_VALUE));
        assertEquals(arbitrary, acResult);

        final ContainerNode cont = createContBuilder(new NodeIdentifier(Arbitrary.QNAME))
                .addChild(createValueBuilder(ARB_VALUE, ARB_NID).build()).build();
        final Esi acmResult = this.parser.serializeEsi(cont);
        assertEquals(arbitrary, acmResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseTest() {
        this.parser.serializeEsi(new AsGeneratedCaseBuilder().build(), null);
    }
}