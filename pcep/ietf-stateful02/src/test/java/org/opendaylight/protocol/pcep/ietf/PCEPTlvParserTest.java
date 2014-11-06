/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspDbVersionTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02NodeIdentifierTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02StatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.node.identifier.tlv.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.node.identifier.tlv.NodeIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;

public class PCEPTlvParserTest {

    private static final byte[] statefulBytes = { 0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01 };
    private static final byte[] symbolicNameBytes = { 0x00, 0x11, 0x00, 0x19, 0x4d, 0x65, 0x64, 0x20, 0x74, 0x65, 0x73, 0x74, 0x20, 0x6f,
        0x66, 0x20, 0x73, 0x79, 0x6d, 0x62, 0x6f, 0x6c, 0x69, 0x63, 0x20, 0x6e, 0x61, 0x6d, 0x65, 0x00, 0x00, 0x00 };
    private static final byte[] nodeIdentifierBytes = { 0x00, 0x18, 0x00, 0x19, 0x4d, 0x65, 0x64, 0x20, 0x74, 0x65, 0x73, 0x74, 0x20, 0x6f,
        0x66, 0x20, 0x73, 0x79, 0x6d, 0x62, 0x6f, 0x6c, 0x69, 0x63, 0x20, 0x6e, 0x61, 0x6d, 0x65, 0x00, 0x00, 0x00 };
    private static final byte[] rsvpErrorBytes = { 0x00, 0x15, 0x00, 0x08, 0x12, 0x34, 0x56, 0x78, 0x02, (byte) 0x92, 0x16, 0x02 };
    private static final byte[] rsvpError6Bytes = { 0x00, 0x15, 0x00, 0x14, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde,
        (byte) 0xf0, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, 0x02, (byte) 0xd5, (byte) 0xc5,
        (byte) 0xd9 };
    private static final byte[] lspDbBytes = { 0x00, 0x17, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xb4 };

    @Test
    public void testStatefulTlv() throws PCEPDeserializerException {
        final Stateful02StatefulCapabilityTlvParser parser = new Stateful02StatefulCapabilityTlvParser();
        final Stateful tlv = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).setIncludeDbVersion(false).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(statefulBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(statefulBytes, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testSymbolicNameTlv() throws PCEPDeserializerException {
        final Stateful02LspSymbolicNameTlvParser parser = new Stateful02LspSymbolicNameTlvParser();
        final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName("Med test of symbolic name".getBytes())).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.subByte(symbolicNameBytes, 4, 25))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(symbolicNameBytes, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testNodeIdentifier() throws PCEPDeserializerException {
        final Stateful02NodeIdentifierTlvParser parser = new Stateful02NodeIdentifierTlvParser();
        final NodeIdentifier tlv = new NodeIdentifierBuilder().setNodeId(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.NodeIdentifier(ByteArray.subByte(
                        nodeIdentifierBytes, 4, 25))).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.subByte(nodeIdentifierBytes, 4, 25))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(nodeIdentifierBytes, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testRSVPError4SpecTlv() throws PCEPDeserializerException {
        final Stateful02RSVPErrorSpecTlvParser parser = new Stateful02RSVPErrorSpecTlvParser();
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        builder.setNode(new IpAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))));
        builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
        builder.setCode((short) 146);
        builder.setValue(5634);
        final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setRsvpError(builder.build()).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.subByte(rsvpErrorBytes, 4, 8))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(rsvpErrorBytes, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testRSVPError6SpecTlv() throws PCEPDeserializerException {
        final Stateful02RSVPErrorSpecTlvParser parser = new Stateful02RSVPErrorSpecTlvParser();
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        builder.setNode(new IpAddress(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
            (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
            (byte) 0xbc, (byte) 0xde, (byte) 0xf0 }))));
        builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
        builder.setCode((short) 213);
        builder.setValue(50649);
        final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setRsvpError(builder.build()).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.subByte(rsvpError6Bytes, 4, 20))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(rsvpError6Bytes, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testLspDbVersionTlv() throws PCEPDeserializerException {
        final Stateful02LspDbVersionTlvParser parser = new Stateful02LspDbVersionTlvParser();
        final LspDbVersion tlv = new LspDbVersionBuilder().setVersion(BigInteger.valueOf(180L)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(lspDbBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(lspDbBytes, ByteArray.getAllBytes(buff));

    }
}
