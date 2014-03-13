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

import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;

import com.google.common.collect.Lists;

public class PCEPTlvParserTest {

	private static final byte[] noPathVectorBytes = { 0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, (byte) 0xa7 };
	private static final byte[] overloadedBytes = { 0x00, 0x02, 0x00, 0x04, 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff };
	private static final byte[] reqMissingBytes = { 0x00, 0x03, 0x00, 0x04, (byte) 0xF7, (byte) 0x82, 0x35, 0x17 };
	private static final byte[] orderBytes = { 0x00, 0x05, 0x00, 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x01 };
	private static final byte[] ofListBytes = { 0x00, 0x04, 0x00, 0x04, 0x12, 0x34, 0x56, 0x78 };

	@Test
	public void testNoPathVectorTlv() throws PCEPDeserializerException {
		final NoPathVectorTlvParser parser = new NoPathVectorTlvParser();
		final NoPathVectorTlv tlv = new NoPathVectorBuilder().setFlags(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true)).build();
		assertEquals(tlv, parser.parseTlv(ByteArray.cutBytes(noPathVectorBytes, 4)));
		assertArrayEquals(noPathVectorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOverloadedDurationTlv() throws PCEPDeserializerException {
		final OverloadedDurationTlvParser parser = new OverloadedDurationTlvParser();
		final OverloadDuration tlv = new OverloadDurationBuilder().setDuration(0x7FFFFFFFL).build();
		assertEquals(tlv, parser.parseTlv(ByteArray.cutBytes(overloadedBytes, 4)));
		assertArrayEquals(overloadedBytes, parser.serializeTlv(tlv));
	}

	public void testReqMissingTlv() throws PCEPDeserializerException {
		final ReqMissingTlvParser parser = new ReqMissingTlvParser();
		final ReqMissing tlv = new ReqMissingBuilder().setRequestId(new RequestId(0xF7823517L)).build();
		assertEquals(tlv, parser.parseTlv(ByteArray.cutBytes(reqMissingBytes, 4)));
		assertArrayEquals(reqMissingBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOrderTlv() throws PCEPDeserializerException {
		final OrderTlvParser parser = new OrderTlvParser();
		final Order tlv = new OrderBuilder().setDelete(0xFFFFFFFFL).setSetup(0x00000001L).build();
		assertEquals(tlv, parser.parseTlv(ByteArray.cutBytes(orderBytes, 4)));
		assertArrayEquals(orderBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOFListTlv() throws PCEPDeserializerException {
		final OFListTlvParser parser = new OFListTlvParser();
		final List<OfId> ids = Lists.newArrayList();
		ids.add(new OfId(0x1234));
		ids.add(new OfId(0x5678));
		final OfList tlv = new OfListBuilder().setCodes(ids).build();
		assertEquals(tlv, parser.parseTlv(ByteArray.cutBytes(ofListBytes, 4)));
		assertArrayEquals(ofListBytes, parser.serializeTlv(tlv));
	}
}
