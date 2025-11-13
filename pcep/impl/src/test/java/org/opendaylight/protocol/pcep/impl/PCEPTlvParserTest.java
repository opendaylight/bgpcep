/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.tlv.AssociationRangeTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.AssociationTypeListTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeCapabilityTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.AssociationRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.AssociationRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.range.tlv.association.range.AssociationRangesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.list.tlv.AssociationTypeListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.srv6.pce.capability.MsdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlvBuilder;
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
    private static final byte[] ASSOCIATION_TYPE_BYTES = { 0x00, 0x23, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00 };
    private static final byte[] ASSOCIATION_RANGE_BYTES = {
        0x00, 0x1d, 0x00, 0x08, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x00, (byte )0xff
    };

    private static final byte[] PST_TLV_BYTES = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0 };

    private static final byte[] PST_TLV_BYTES_UNSUPPORTED = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x8 };

    private static final byte[] PST_CAPA_BYTES = {
        0x00, 0x22, 0x00, 0x1c, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x03, 0x00,
        0x00, 0x1a, 0x00, 0x04, 0x00, 0x00, 0x01, 0x0a,
        0x00, 0x1b, 0x00, 0x06, 0x00, 0x00, 0x00, 0x02, 0x01, 0x0a, 0x00, 0x00
    };

    @Test
    public void testNoPathVectorTlv() throws PCEPDeserializerException {
        final NoPathVectorTlvParser parser = new NoPathVectorTlvParser();
        final NoPathVectorTlv tlv = new NoPathVectorBuilder()
                .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
                    .NoPathVectorTlv.Flags(false, false, false, true, false, true, false, true, true, true))
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
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst(PsType.RsvpTe).build();
        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
        assertArrayEquals(PST_TLV_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testUnsupportedPSTParser() {
        final var parser = new PathSetupTypeTlvParser();
        final var ex = assertThrows(PCEPDeserializerException.class,
            () -> parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES_UNSUPPORTED, 4))));
        assertEquals("Unsuported Path Setup Type: 8", ex.getMessage());
    }

    @Test
    public void testPathSetupTypeCapabilityTlvParser() throws PCEPDeserializerException {
        final PathSetupTypeCapabilityTlvParser parser = new PathSetupTypeCapabilityTlvParser();
        final PathSetupTypeCapability pstCapability = new PathSetupTypeCapabilityBuilder()
            .setPsts(List.of(PsType.RsvpTe, PsType.SrMpls, PsType.Srv6))
            .setSrPceCapability(new SrPceCapabilityBuilder()
                .setMsd(Uint8.TEN)
                .setNFlag(false)
                .setXFlag(true)
                .build())
            .setSrv6PceCapability(new Srv6PceCapabilityBuilder()
                 .setNFlag(true)
                 .setMsds(List.of(new MsdsBuilder().setMsdType(Uint8.ONE).setMsdValue(Uint8.TEN).build()))
                 .build())
            .build();
        assertEquals(pstCapability, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_CAPA_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstCapability, buff);
        assertArrayEquals(PST_CAPA_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testAssociationTypeListTlvParser()  throws PCEPDeserializerException {
        final AssociationTypeListTlvParser parser = new AssociationTypeListTlvParser();
        final AssociationTypeList assocTypeList = new AssociationTypeListBuilder()
            .setAssociationType(Set.of(AssociationType.Disjoint)).build();
        assertEquals(assocTypeList,
            parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.subByte(ASSOCIATION_TYPE_BYTES, 4, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(assocTypeList, buff);
        assertArrayEquals(ASSOCIATION_TYPE_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testAssociationRangeTlvParser()  throws PCEPDeserializerException {
        final AssociationRangeTlvParser parser = new AssociationRangeTlvParser();
        final AssociationRange assocRangeList = new AssociationRangeBuilder()
            .setAssociationRanges(List.of(new AssociationRangesBuilder()
                .setAssociationType(AssociationType.Disjoint)
                .setAssociationIdStart(Uint16.ONE)
                .setRange(Uint16.valueOf(255))
                .build()))
            .build();
        assertEquals(assocRangeList,
            parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(ASSOCIATION_RANGE_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(assocRangeList, buff);
        assertArrayEquals(ASSOCIATION_RANGE_BYTES, ByteArray.getAllBytes(buff));
    }
}
