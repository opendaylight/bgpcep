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

import java.math.BigInteger;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspDbVersionTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PredundancyGroupTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

import com.google.common.collect.Lists;

public class PCEPTlvParserTest {

	private static final byte[] statefulBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02 };
	private static final byte[] DbVersionBytes = { (byte) 0xff, (byte) 0x00, (byte) 0xff, (byte) 0xaa, (byte) 0xb2, (byte) 0xf5,
		(byte) 0xf2, (byte) 0xcf };
	private static final byte[] noPathVectorBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xa7 };
	private static final byte[] overloadedBytes = { (byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff };
	private static final byte[] symbolicNameBytes = { (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65,
		(byte) 0x73, (byte) 0x74, (byte) 0x20, (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d,
		(byte) 0x62, (byte) 0x6f, (byte) 0x6c, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d,
		(byte) 0x65 };
	private static final byte[] lspUpdateErrorBytes = { (byte) 0x25, (byte) 0x68, (byte) 0x95, (byte) 0x03 };
	private static final byte[] lspIdentifiers4Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xFF, (byte) 0xFF,
		(byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };
	private static final byte[] lspIdentifiers6Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC,
		(byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
		(byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0xFF, (byte) 0xFF, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
		(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x01,
		(byte) 0x23, (byte) 0x45, (byte) 0x67 };
	private static final byte[] rsvpErrorBytes = { (byte) 0x06, (byte) 0x01, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
		(byte) 0x02, (byte) 0x92, (byte) 0x16, (byte) 0x02 };
	private static final byte[] rsvpError6Bytes = { (byte) 0x06, (byte) 0x02, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
		(byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
		(byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x02, (byte) 0xd5, (byte) 0xc5, (byte) 0xd9 };
	private static final byte[] userErrorBytes = { (byte) 0xc2, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x39,
		(byte) 0x05, (byte) 0x09, (byte) 0x00, (byte) 0x26, (byte) 0x75, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x20,
		(byte) 0x64, (byte) 0x65, (byte) 0x73, (byte) 0x63 };
	private static final byte[] reqMissingBytes = { (byte) 0xF7, (byte) 0x82, (byte) 0x35, (byte) 0x17 };
	private static final byte[] orderBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x01 };
	private static final byte[] ofListBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };
	private static final byte[] predundancyBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };

	@Test
	public void testStatefulTlv() throws PCEPDeserializerException {
		final PCEStatefulCapabilityTlvParser parser = new PCEStatefulCapabilityTlvParser();
		final Stateful tlv = new StatefulBuilder().setLspUpdateCapability(Boolean.FALSE).setIncludeDbVersion(Boolean.TRUE).build();
		assertEquals(tlv, parser.parseTlv(statefulBytes));
		assertArrayEquals(statefulBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testStateDbVersionTlv() throws PCEPDeserializerException {
		final LspDbVersionTlvParser parser = new LspDbVersionTlvParser();
		final LspDbVersion tlv = new LspDbVersionBuilder().setVersion(BigInteger.valueOf(0xFF00FFAAB2F5F2CFL)).build();
		assertEquals(tlv, parser.parseTlv(DbVersionBytes));
		assertArrayEquals(DbVersionBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testNoPathVectorTlv() throws PCEPDeserializerException {
		final NoPathVectorTlvParser parser = new NoPathVectorTlvParser();
		final NoPathVectorTlv tlv = new NoPathVectorBuilder().setFlags(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true)).build();
		assertEquals(tlv, parser.parseTlv(noPathVectorBytes));
		assertArrayEquals(noPathVectorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOverloadedDurationTlv() throws PCEPDeserializerException {
		final OverloadedDurationTlvParser parser = new OverloadedDurationTlvParser();
		final OverloadDuration tlv = new OverloadDurationBuilder().setDuration(0x7FFFFFFFL).build();
		assertEquals(tlv, parser.parseTlv(overloadedBytes));
		assertArrayEquals(overloadedBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testSymbolicNameTlv() throws PCEPDeserializerException {
		final LspSymbolicNameTlvParser parser = new LspSymbolicNameTlvParser();
		final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathName("Med test of symbolic name".getBytes())).build();
		assertEquals(tlv, parser.parseTlv(symbolicNameBytes));
		assertArrayEquals(symbolicNameBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspErrorCodeTlv() throws PCEPDeserializerException {
		final LspUpdateErrorTlvParser parser = new LspUpdateErrorTlvParser();
		final LspErrorCode tlv = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
		assertEquals(tlv, parser.parseTlv(lspUpdateErrorBytes));
		assertArrayEquals(lspUpdateErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspIdentifiers4Tlv() throws PCEPDeserializerException {
		final LSPIdentifierTlvParser parser = new LSPIdentifierTlvParser();
		final Ipv4Builder afi = new Ipv4Builder();
		afi.setIpv4TunnelSenderAddress(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }));
		afi.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56,
				(byte) 0x78 })));
		final LspIdentifiers tlv = new LspIdentifiersBuilder().setAddressFamily(afi.build()).setLspId(new LspId(65535L)).setTunnelId(
				new TunnelId(4660)).build();
		assertEquals(tlv, parser.parseTlv(lspIdentifiers4Bytes));
		assertArrayEquals(lspIdentifiers4Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspIdentifiers6Tlv() throws PCEPDeserializerException {
		final LSPIdentifierTlvParser parser = new LSPIdentifierTlvParser();
		final Ipv6Builder afi = new Ipv6Builder();
		afi.setIpv6TunnelSenderAddress(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
				(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A,
				(byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }));
		afi.setIpv6ExtendedTunnelId(new Ipv6ExtendedTunnelId(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56,
				(byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
				(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67 })));
		final LspIdentifiers tlv = new LspIdentifiersBuilder().setAddressFamily(afi.build()).setLspId(new LspId(4660L)).setTunnelId(
				new TunnelId(65535)).build();
		assertEquals(tlv, parser.parseTlv(lspIdentifiers6Bytes));
		assertArrayEquals(lspIdentifiers6Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testRSVPError4SpecTlv() throws PCEPDeserializerException {
		final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		builder.setNode(new IpAddress(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
		builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
		builder.setCode((short) 146);
		builder.setValue(5634);
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new RsvpBuilder().setRsvpError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(rsvpErrorBytes));
		assertArrayEquals(rsvpErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testRSVPError6SpecTlv() throws PCEPDeserializerException {
		final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		builder.setNode(new IpAddress(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
				(byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
				(byte) 0xbc, (byte) 0xde, (byte) 0xf0 })));
		builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
		builder.setCode((short) 213);
		builder.setValue(50649);
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new RsvpBuilder().setRsvpError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(rsvpError6Bytes));
		assertArrayEquals(rsvpError6Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testUserErrorSpecTlv() throws PCEPDeserializerException {
		final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
		final UserErrorBuilder builder = new UserErrorBuilder();
		builder.setEnterprise(new EnterpriseNumber(12345L));
		builder.setSubOrg((short) 5);
		builder.setValue(38);
		builder.setDescription("user desc");
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new UserBuilder().setUserError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(userErrorBytes));
		assertArrayEquals(userErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testReqMissingTlv() throws PCEPDeserializerException {
		final ReqMissingTlvParser parser = new ReqMissingTlvParser();
		final ReqMissing tlv = new ReqMissingBuilder().setRequestId(new RequestId(0xF7823517L)).build();
		assertEquals(tlv, parser.parseTlv(reqMissingBytes));
		assertArrayEquals(reqMissingBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOrderTlv() throws PCEPDeserializerException {
		final OrderTlvParser parser = new OrderTlvParser();
		final Order tlv = new OrderBuilder().setDelete(0xFFFFFFFFL).setSetup(0x00000001L).build();
		assertEquals(tlv, parser.parseTlv(orderBytes));
		assertArrayEquals(orderBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testOFListTlv() throws PCEPDeserializerException {
		final OFListTlvParser parser = new OFListTlvParser();
		final List<OfId> ids = Lists.newArrayList();
		ids.add(new OfId(0x1234));
		ids.add(new OfId(0x5678));
		final OfList tlv = new OfListBuilder().setCodes(ids).build();
		assertEquals(tlv, parser.parseTlv(ofListBytes));
		assertArrayEquals(ofListBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testPredundancyTlv() throws PCEPDeserializerException {
		final PredundancyGroupTlvParser parser = new PredundancyGroupTlvParser();
		final PredundancyGroupId tlv = new PredundancyGroupIdBuilder().setIdentifier(predundancyBytes).build();
		assertEquals(tlv, parser.parseTlv(predundancyBytes));
		assertArrayEquals(predundancyBytes, parser.serializeTlv(tlv));
	}
}
