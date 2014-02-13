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

import java.math.BigInteger;

import org.junit.Test;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspDbVersionTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02StatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;

public class PCEPTlvParserTest {

	private static final byte[] statefulBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
	private static final byte[] symbolicNameBytes = { (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65,
			(byte) 0x73, (byte) 0x74, (byte) 0x20, (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d,
			(byte) 0x62, (byte) 0x6f, (byte) 0x6c, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d,
			(byte) 0x65 };
	private static final byte[] rsvpErrorBytes = { (byte) 0x06, (byte) 0x01, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x02, (byte) 0x92, (byte) 0x16, (byte) 0x02 };
	private static final byte[] rsvpError6Bytes = { (byte) 0x06, (byte) 0x02, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a,
			(byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x02, (byte) 0xd5, (byte) 0xc5, (byte) 0xd9 };
	private static final byte[] userErrorBytes = { (byte) 0xc2, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x39,
			(byte) 0x05, (byte) 0x09, (byte) 0x00, (byte) 0x26, (byte) 0x75, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x20,
			(byte) 0x64, (byte) 0x65, (byte) 0x73, (byte) 0x63 };
	private static final byte[] lspDbBytes = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0xb4 };

	@Test
	public void testStatefulTlv() throws PCEPDeserializerException {
		final Stateful02StatefulCapabilityTlvParser parser = new Stateful02StatefulCapabilityTlvParser();
		final Stateful tlv = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).setIncludeDbVersion(false).build();
		assertEquals(tlv, parser.parseTlv(statefulBytes));
		assertArrayEquals(statefulBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testSymbolicNameTlv() throws PCEPDeserializerException {
		final Stateful02LspSymbolicNameTlvParser parser = new Stateful02LspSymbolicNameTlvParser();
		final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName("Med test of symbolic name".getBytes())).build();
		assertEquals(tlv, parser.parseTlv(symbolicNameBytes));
		assertArrayEquals(symbolicNameBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testRSVPError4SpecTlv() throws PCEPDeserializerException {
		final Stateful02RSVPErrorSpecTlvParser parser = new Stateful02RSVPErrorSpecTlvParser();
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
		final Stateful02RSVPErrorSpecTlvParser parser = new Stateful02RSVPErrorSpecTlvParser();
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
		final Stateful02RSVPErrorSpecTlvParser parser = new Stateful02RSVPErrorSpecTlvParser();
		final UserErrorBuilder builder = new UserErrorBuilder();
		builder.setEnterprise(new EnterpriseNumber(12345L));
		builder.setSubOrg((short) 5);
		builder.setValue(38);
		builder.setDescription("user desc");
		final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder().setErrorType(new UserCaseBuilder().setUserError(builder.build()).build()).build();
		assertEquals(tlv, parser.parseTlv(userErrorBytes));
		assertArrayEquals(userErrorBytes, parser.serializeTlv(tlv));
	}

	@Test
	public void testLspDbVersionTlv() throws PCEPDeserializerException {
		final Stateful02LspDbVersionTlvParser parser = new Stateful02LspDbVersionTlvParser();
		final LspDbVersion tlv = new LspDbVersionBuilder().setVersion(BigInteger.valueOf(180L)).build();
		assertEquals(tlv, parser.parseTlv(lspDbBytes));
		assertArrayEquals(lspDbBytes, parser.serializeTlv(tlv));

	}
}
