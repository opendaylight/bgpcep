/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPTlvParserTest {
    private static final byte[] NO_PATH_VECTOR_BYTES = {
        0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, (byte) 0xa7
    };
    private static final byte[] OVERLOADED_BYTES = {
        0x00, 0x02, 0x00, 0x04, 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff
    };
    private static final byte[] REQ_MISSING_BYTES = {
        0x00, 0x03, 0x00, 0x04, (byte) 0xF7, (byte) 0x82, 0x35, 0x17
    };
    private static final byte[] ORDER_BYTES = {
        0x00, 0x05, 0x00, 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x01
    };
    private static final byte[] OF_LIST_BYTES = { 0x00, 0x04, 0x00, 0x04, 0x12, 0x34, 0x56, 0x78 };
    private static final byte[] VENDOR_INFO_BYTES = {
        0x00, 0x07, 0x00, 0x08,
        /* Enterprise number */
        0x00, 0x00, 0x00, 0x00,
        /* Enterprise specific information */
        0x00, 0x00, 0x00, 0x05
    };

    private static final byte[] PST_TLV_BYTES = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0 };

    private static final byte[] PST_TLV_BYTES_UNSUPPORTED = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    @Test
    public void testNoPathVectorTlv() throws PCEPDeserializerException {
        final NoPathVectorTlvParser parser = new NoPathVectorTlvParser();
        final NoPathVectorTlv tlv = new NoPathVectorBuilder()
                .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true))
                .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(NO_PATH_VECTOR_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(NO_PATH_VECTOR_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOverloadedDurationTlv() throws PCEPDeserializerException {
        final OverloadedDurationTlvParser parser = new OverloadedDurationTlvParser();
        final OverloadDuration tlv = new OverloadDurationBuilder().setDuration(Uint32.valueOf(0x7FFFFFFFL)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(OVERLOADED_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(OVERLOADED_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testReqMissingTlv() throws PCEPDeserializerException {
        final ReqMissingTlvParser parser = new ReqMissingTlvParser();
        final ReqMissing tlv = new ReqMissingBuilder().setRequestId(new RequestId(Uint32.valueOf(0xF7823517L))).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(REQ_MISSING_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(REQ_MISSING_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOrderTlv() throws PCEPDeserializerException {
        final OrderTlvParser parser = new OrderTlvParser();
        final Order tlv = new OrderBuilder()
                .setDelete(Uint32.valueOf(0xFFFFFFFFL))
                .setSetup(Uint32.valueOf(0x00000001L))
                .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(ORDER_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(ORDER_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOFListTlv() throws PCEPDeserializerException {
        final OFListTlvParser parser = new OFListTlvParser();
        final OfList tlv = new OfListBuilder()
            // Predictable order
            .setCodes(ImmutableSet.of(new OfId(Uint16.valueOf(0x1234)), new OfId(Uint16.valueOf(0x5678))))
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(OF_LIST_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(OF_LIST_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testVendorInformationTlv() throws PCEPDeserializerException {
        final TestVendorInformationTlvParser parser = new TestVendorInformationTlvParser();
        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationTlv viTlv = new VendorInformationTlvBuilder()
                .setEnterpriseNumber(new EnterpriseNumber(Uint32.ZERO))
                .setEnterpriseSpecificInformation(esInfo)
                .build();

        final VendorInformationTlv parsedTlv = parser.parseTlv(
            Unpooled.wrappedBuffer(ByteArray.cutBytes(VENDOR_INFO_BYTES, 8)));
        assertEquals(viTlv, parsedTlv);

        final ByteBuf buff = Unpooled.buffer(VENDOR_INFO_BYTES.length);
        parser.serializeTlv(viTlv, buff);
        assertArrayEquals(VENDOR_INFO_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testPathSetupTypeTlvParser() throws PCEPDeserializerException {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst(Uint8.ZERO).build();
        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
        assertArrayEquals(PST_TLV_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test(expected = PCEPDeserializerException.class)
    public void testUnsupportedPSTParser() throws PCEPDeserializerException {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES_UNSUPPORTED, 4)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedPSTSerializer() {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst(Uint8.ONE).build();
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
    }
}
