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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.tlv.AbstractVendorSpecificTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.VsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vs.tlv.vs.tlv.VendorPayload;
import org.opendaylight.yangtools.yang.binding.DataContainer;

@SuppressWarnings("ALL")
public class PCEPTlvParserTest {

    private static final byte[] noPathVectorBytes = { 0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, (byte) 0xa7 };
    private static final byte[] overloadedBytes = { 0x00, 0x02, 0x00, 0x04, 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff };
    private static final byte[] reqMissingBytes = { 0x00, 0x03, 0x00, 0x04, (byte) 0xF7, (byte) 0x82, 0x35, 0x17 };
    private static final byte[] orderBytes = { 0x00, 0x05, 0x00, 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00,
        0x00, 0x01 };
    private static final byte[] ofListBytes = { 0x00, 0x04, 0x00, 0x04, 0x12, 0x34, 0x56, 0x78 };
    private static final byte[] vsTlvBytes = { 0x00, 0x1b, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x09, 0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00,
        0x05 };
    private static final byte[] VENDOR_INFO_BYTES = {
        0x00, 0x07, 0x00, 0x08,
        /* Enterprise number */
        0x00, 0x00, 0x00, 0x00,
        /* Enterprise specific information */
        0x00, 0x00, 0x00, 0x05
    };

    private static final byte[] PST_TLV_BYTES = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0 };

    private static final byte[] PST_TLV_BYTES_UNSUPPORTED = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    private final AbstractVendorSpecificTlvParser vsParser = new AbstractVendorSpecificTlvParser() {

        @Override
        protected void serializeVendorPayload(final VendorPayload payload, final ByteBuf buffer) {
            buffer.writeBytes(new byte[] { 0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, 0x05 });
        }

        @Override
        protected VendorPayload parseVendorPayload(final ByteBuf payloadBytes) throws PCEPDeserializerException {
            return PCEPTlvParserTest.this.vp;
        }

        @Override
        protected long getEnterpriseNumber() {
            return 9;
        }
    };

    private final VendorPayload vp = new VendorPayload() {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    };

    @Test
    public void testNoPathVectorTlv() throws PCEPDeserializerException {
        final NoPathVectorTlvParser parser = new NoPathVectorTlvParser();
        final NoPathVectorTlv tlv = new NoPathVectorBuilder().setFlags(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(noPathVectorBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(noPathVectorBytes, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOverloadedDurationTlv() throws PCEPDeserializerException {
        final OverloadedDurationTlvParser parser = new OverloadedDurationTlvParser();
        final OverloadDuration tlv = new OverloadDurationBuilder().setDuration(0x7FFFFFFFL).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(overloadedBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(overloadedBytes, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testReqMissingTlv() throws PCEPDeserializerException {
        final ReqMissingTlvParser parser = new ReqMissingTlvParser();
        final ReqMissing tlv = new ReqMissingBuilder().setRequestId(new RequestId(0xF7823517L)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(reqMissingBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(reqMissingBytes, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOrderTlv() throws PCEPDeserializerException {
        final OrderTlvParser parser = new OrderTlvParser();
        final Order tlv = new OrderBuilder().setDelete(0xFFFFFFFFL).setSetup(0x00000001L).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(orderBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(orderBytes, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testOFListTlv() throws PCEPDeserializerException {
        final OFListTlvParser parser = new OFListTlvParser();
        final List<OfId> ids = Lists.newArrayList();
        ids.add(new OfId(0x1234));
        ids.add(new OfId(0x5678));
        final OfList tlv = new OfListBuilder().setCodes(ids).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(ofListBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(ofListBytes, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testVendorSpecificTlv() throws PCEPDeserializerException {
        final VsTlv tlv = new VsTlvBuilder().setEnterpriseNumber(new EnterpriseNumber(9L)).setVendorPayload(this.vp).build();
        assertEquals(tlv, this.vsParser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(vsTlvBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        this.vsParser.serializeTlv(tlv, buff);
        assertArrayEquals(vsTlvBytes, ByteArray.getAllBytes(buff));
        assertNull(this.vsParser.parseTlv(null));
    }

    @Test
    public void testVendorInformationTlv() throws PCEPDeserializerException {
        final TestVendorInformationTlvParser parser = new TestVendorInformationTlvParser();
        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationTlv viTlv = new VendorInformationTlvBuilder().setEnterpriseNumber(new EnterpriseNumber(0L))
                .setEnterpriseSpecificInformation(esInfo).build();

        final VendorInformationTlv parsedTlv = parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(VENDOR_INFO_BYTES, 8)));
        assertEquals(viTlv, parsedTlv);

        final ByteBuf buff = Unpooled.buffer(VENDOR_INFO_BYTES.length);
        parser.serializeTlv(viTlv, buff);
        assertArrayEquals(VENDOR_INFO_BYTES, ByteArray.getAllBytes(buff));
        assertNull(parser.parseTlv(null));
    }

    @Test
    public void testPathSetupTypeTlvParser() throws PCEPDeserializerException {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst((short) 0).build();
        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
        assertArrayEquals(PST_TLV_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test(expected=PCEPDeserializerException.class)
    public void testUnsupportedPSTParser() throws PCEPDeserializerException {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(PST_TLV_BYTES_UNSUPPORTED, 4)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnsupportedPSTSerializer() {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst((short) 1).build();
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
    }
}
