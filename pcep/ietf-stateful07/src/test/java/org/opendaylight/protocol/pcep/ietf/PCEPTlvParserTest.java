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

import org.junit.Test;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LSPIdentifierIpv4TlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LSPIdentifierIpv6TlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07StatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

public class PCEPTlvParserTest {

	private static final byte[] statefulBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
	private static final byte[] symbolicNameBytes = { (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65,
			(byte) 0x73, (byte) 0x74, (byte) 0x20, (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d,
			(byte) 0x62, (byte) 0x6f, (byte) 0x6c, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d,
			(byte) 0x65 };
	private static final byte[] lspUpdateErrorBytes = { (byte) 0x25, (byte) 0x68, (byte) 0x95, (byte) 0x03 };
	private static final byte[] lspIdentifiers4Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xFF, (byte) 0xFF,
			(byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56,
			(byte) 0x78 };
	private static final byte[] lspIdentifiers6Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC,
			(byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
			(byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0xFF, (byte) 0xFF, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x01,
			(byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC,
			(byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
			(byte) 0xF0 };
	private static final byte[] rsvpErrorBytes = { (byte) 0x06, (byte) 0x01, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x02, (byte) 0x92, (byte) 0x16, (byte) 0x02 };
	private static final byte[] rsvpError6Bytes = { (byte) 0x06, (byte) 0x02, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
			(byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x02, (byte) 0xd5, (byte) 0xc5, (byte) 0xd9 };
	private static final byte[] userErrorBytes = { (byte) 0xc2, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x39,
			(byte) 0x05, (byte) 0x09, (byte) 0x00, (byte) 0x26, (byte) 0x75, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x20,
			(byte) 0x64, (byte) 0x65, (byte) 0x73, (byte) 0x63 };

	@Test
	public void testStatefulTlv() throws PCEPDeserializerException {
		final Stateful07StatefulCapabilityTlvParser parser = new Stateful07StatefulCapabilityTlvParser();
		final Stateful tlv = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).build();
		assertEquals(tlv, parser.parseTlv(statefulBytes));
		assertArrayEquals(statefulBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testSymbolicNameTlv() throws PCEPDeserializerException {
		final Stateful07LspSymbolicNameTlvParser parser = new Stateful07LspSymbolicNameTlvParser();
		final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName("Med test of symbolic name".getBytes())).build();
		assertEquals(tlv, parser.parseTlv(symbolicNameBytes));
		assertArrayEquals(symbolicNameBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspErrorCodeTlv() throws PCEPDeserializerException {
		final Stateful07LspUpdateErrorTlvParser parser = new Stateful07LspUpdateErrorTlvParser();
		final LspErrorCode tlv = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
		assertEquals(tlv, parser.parseTlv(lspUpdateErrorBytes));
		assertArrayEquals(lspUpdateErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspIdentifiers4Tlv() throws PCEPDeserializerException {
		final Stateful07LSPIdentifierIpv4TlvParser parser = new Stateful07LSPIdentifierIpv4TlvParser();
		final Ipv4Builder afi = new Ipv4Builder();
		afi.setIpv4TunnelSenderAddress(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }));
		afi.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56,
				(byte) 0x78 })));
		afi.setIpv4TunnelEndpointAddress(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }));
		final LspIdentifiers tlv = new LspIdentifiersBuilder().setAddressFamily(new Ipv4CaseBuilder().setIpv4(afi.build()).build()).setLspId(
				new LspId(65535L)).setTunnelId(new TunnelId(4660)).build();
		assertEquals(tlv, parser.parseTlv(lspIdentifiers4Bytes));
		assertArrayEquals(lspIdentifiers4Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspIdentifiers6Tlv() throws PCEPDeserializerException {
		final Stateful07LSPIdentifierIpv6TlvParser parser = new Stateful07LSPIdentifierIpv6TlvParser();
		final Ipv6Builder afi = new Ipv6Builder();
		afi.setIpv6TunnelSenderAddress(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
				(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A,
				(byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }));
		afi.setIpv6ExtendedTunnelId(new Ipv6ExtendedTunnelId(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56,
				(byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
				(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67 })));
		afi.setIpv6TunnelEndpointAddress(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
				(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A,
				(byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }));
		final LspIdentifiers tlv = new LspIdentifiersBuilder().setAddressFamily(new Ipv6CaseBuilder().setIpv6(afi.build()).build()).setLspId(
				new LspId(4660L)).setTunnelId(new TunnelId(65535)).build();
		assertEquals(tlv, parser.parseTlv(lspIdentifiers6Bytes));
		assertArrayEquals(lspIdentifiers6Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testRSVPError4SpecTlv() throws PCEPDeserializerException {
		final Stateful07RSVPErrorSpecTlvParser parser = new Stateful07RSVPErrorSpecTlvParser();
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		builder.setNode(new IpAddress(Ipv4Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
		builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
		builder.setCode((short) 146);
		builder.setValue(5634);
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new RsvpCaseBuilder().setRsvpError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(rsvpErrorBytes));
		assertArrayEquals(rsvpErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testRSVPError6SpecTlv() throws PCEPDeserializerException {
		final Stateful07RSVPErrorSpecTlvParser parser = new Stateful07RSVPErrorSpecTlvParser();
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		builder.setNode(new IpAddress(Ipv6Util.addressForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
				(byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
				(byte) 0xbc, (byte) 0xde, (byte) 0xf0 })));
		builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags(false, true));
		builder.setCode((short) 213);
		builder.setValue(50649);
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new RsvpCaseBuilder().setRsvpError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(rsvpError6Bytes));
		assertArrayEquals(rsvpError6Bytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testUserErrorSpecTlv() throws PCEPDeserializerException {
		final Stateful07RSVPErrorSpecTlvParser parser = new Stateful07RSVPErrorSpecTlvParser();
		final UserErrorBuilder builder = new UserErrorBuilder();
		builder.setEnterprise(new EnterpriseNumber(12345L));
		builder.setSubOrg((short) 5);
		builder.setValue(38);
		builder.setDescription("user desc");
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new UserCaseBuilder().setUserError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(userErrorBytes));
		assertArrayEquals(userErrorBytes, parser.serializeTlv(tlv));
	}
}
