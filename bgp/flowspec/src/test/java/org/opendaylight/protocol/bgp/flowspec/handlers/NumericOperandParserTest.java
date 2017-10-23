/*
 * Copyright (c) 2017 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.handlers;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;

public class NumericOperandParserTest {
    private static final byte[] ONE_BYTE_CODE_LIST = new byte[]{
            0x01, 0x64,
            0x01, 0x65,
            (byte) 0x81, 0x66,    // last port in the list should have end-of-list set
    };

    @Test
    public void testSerializeTwoByte() {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        final List<Ports> ports = new ArrayList<>();
        // create 3 ports without end-of-list bit set
        for (int i = 0; i < 3; i++) {
            ports.add(
                    new PortsBuilder()
                            .setOp(new NumericOperand(false, false, true, false, false))
                            .setValue(100 + i)
                            .build()
            );
        }
        NumericTwoByteOperandParser.INSTANCE.serialize(ports, nlriByteBuf);
        assertArrayEquals(ONE_BYTE_CODE_LIST, ByteArray.readAllBytes(nlriByteBuf));
    }

    @Test
    public void testSerializeOneByte() {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        final List<Codes> codes = new ArrayList<>();
        // create 3 ports without end-of-list bit set
        for (int i = 0; i < 3; i++) {
            codes.add(
                    new CodesBuilder()
                            .setOp(new NumericOperand(false, false, true, false, false))
                            .setValue((short) (100 + i))
                            .build()
            );
        }
        NumericOneByteOperandParser.INSTANCE.serialize(codes, nlriByteBuf);
        assertArrayEquals(ONE_BYTE_CODE_LIST, ByteArray.readAllBytes(nlriByteBuf));
    }

    @Test
    public void testSerializeVariableByte() {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        // test with a operand with endOfList set to true, but override with false
        NumericOneByteOperandParser.INSTANCE.serialize(
                new NumericOperand(false, true, true, false, false),
                1,
                false,
                nlriByteBuf);
        assertArrayEquals(new byte[]{(byte) 0x01}, ByteArray.readAllBytes(nlriByteBuf));

        // test with a operand with endOfList set to false, but override with true
        nlriByteBuf.clear();
        NumericOneByteOperandParser.INSTANCE.serialize(
                new NumericOperand(false, true, true, false, false),
                1,
                true,
                nlriByteBuf);
        assertArrayEquals(new byte[]{(byte) 0x81}, ByteArray.readAllBytes(nlriByteBuf));
    }
}