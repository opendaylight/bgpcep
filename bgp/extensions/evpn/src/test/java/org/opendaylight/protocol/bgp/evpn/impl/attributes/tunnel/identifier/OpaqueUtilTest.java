/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.Opaque;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValueBuilder;

public class OpaqueUtilTest {
    private static final byte[] OPAQUE_WRONG = {
        (byte) 0xfc, (byte) 0x00, (byte) 0x03, // Opaque Type - Length
        (byte) 0xb5, (byte) 0xeb, (byte) 0x2d,  //Value
    };

    private static final byte[] OPAQUE_EXPECTED = {
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00
    };
    private static final byte[] OPAQUE_EXT_EXPECTED = {
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };
    private static final byte[] OPAQUE_VALUES_EXPECTED = {
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };

    static final HexString OPAQUE_TEST = new HexString("07:00:0b:00:00:01:00:00:00:01:00:00:00:00");
    static final HexString OPAQUE_TEST2 = new HexString("07:00:0b:00:00:01:00:00:00:01:00:00:00:00:01:02");
    private static final Opaque OPAQUE = new OpaqueValueBuilder().setOpaque(OPAQUE_TEST)
            .setOpaqueType(OpaqueUtil.GENERIC_LSP_IDENTIFIER).build();
    private static final Opaque OPAQUE_EXTENDED = new OpaqueValueBuilder().setOpaque(OPAQUE_TEST2)
            .setOpaqueType((short) 2).setOpaqueType(OpaqueUtil.EXTENDED_TYPE).setOpaqueExtendedType(4).build();
    private static final List<OpaqueValue> OPAQUE_VALUE_LIST = Arrays.asList((OpaqueValue) OPAQUE,
            (OpaqueValue) OPAQUE_EXTENDED);

    @Test
    public void serializeOpaque() {
        final ByteBuf actualOpaque = Unpooled.buffer();
        OpaqueUtil.serializeOpaque(OPAQUE, actualOpaque);
        assertArrayEquals(OPAQUE_EXPECTED, ByteArray.readAllBytes(actualOpaque));

        final ByteBuf actualOpaqueExt = Unpooled.buffer();
        OpaqueUtil.serializeOpaque(OPAQUE_EXTENDED, actualOpaqueExt);
        assertArrayEquals(OPAQUE_EXT_EXPECTED, ByteArray.readAllBytes(actualOpaqueExt));

        final ByteBuf empty = Unpooled.buffer();
        OpaqueUtil.serializeOpaque(new OpaqueValueBuilder().setOpaqueType((short) 5).build(), actualOpaqueExt);
        assertArrayEquals(new byte[0], ByteArray.readAllBytes(empty));

        final Opaque opaque = OpaqueUtil.parseOpaque(Unpooled.wrappedBuffer(OPAQUE_EXPECTED));
        assertEquals(OPAQUE, opaque);

        final Opaque opaqueExt = OpaqueUtil.parseOpaque(Unpooled.wrappedBuffer(OPAQUE_EXT_EXPECTED));
        assertEquals(OPAQUE_EXTENDED, opaqueExt);

        assertNull(OpaqueUtil.parseOpaque(Unpooled.wrappedBuffer(OPAQUE_WRONG)));
    }

    @Test
    public void parseOpaqueList() {
        final ByteBuf opaqueValues = Unpooled.buffer();
        OpaqueUtil.serializeOpaqueList(OPAQUE_VALUE_LIST, opaqueValues);
        assertArrayEquals(OPAQUE_VALUES_EXPECTED, ByteArray.readAllBytes(opaqueValues));
        assertEquals(OPAQUE_VALUE_LIST, OpaqueUtil.parseOpaqueList(Unpooled.wrappedBuffer(OPAQUE_VALUES_EXPECTED)));
    }
}