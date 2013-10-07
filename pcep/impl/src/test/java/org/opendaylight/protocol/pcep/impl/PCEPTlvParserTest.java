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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.concepts.IPv4ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.IPv6ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv6TlvParser;
import org.opendaylight.protocol.pcep.tlv.IPv4LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.IPv6LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.pcep.tlv.LSPSymbolicNameTlv;
import org.opendaylight.protocol.pcep.tlv.LSPUpdateErrorTlv;
import org.opendaylight.protocol.pcep.tlv.NoPathVectorTlv;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;
import org.opendaylight.protocol.pcep.tlv.OFListTlv;
import org.opendaylight.protocol.pcep.tlv.OrderTlv;
import org.opendaylight.protocol.pcep.tlv.OverloadedDurationTlv;
import org.opendaylight.protocol.pcep.tlv.P2MPCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv;
import org.opendaylight.protocol.pcep.tlv.ReqMissingTlv;

/**
 * Tests of PCEPTlvParser
 */
public class PCEPTlvParserTest {

    @Test
    public void testDeserialization() throws PCEPDeserializerException, IOException {
	final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PackOfTlvs.bin");
	final List<PCEPTlv> tlvsToTest = PCEPTlvParser.parseTlv(bytesFromFile);

	assertEquals(17, tlvsToTest.size());
	assertEquals(tlvsToTest.get(0), new PCEStatefulCapabilityTlv(false, false, true));
	assertEquals(tlvsToTest.get(1), new LSPStateDBVersionTlv(0xFF00FFAAB2F5F2CFL));
	assertEquals(tlvsToTest.get(2), new PCEStatefulCapabilityTlv(false, true, true));
	assertEquals(tlvsToTest.get(3), new LSPStateDBVersionTlv(0xFFFFFFFFFFFFFFFFL));
	assertEquals(tlvsToTest.get(4), new NoPathVectorTlv(true, true, true, false, true, true));
	assertEquals(tlvsToTest.get(5), new OverloadedDurationTlv(0x7FFFFFFF));
	assertEquals(tlvsToTest.get(6), new LSPSymbolicNameTlv(new LSPSymbolicName(new String("Med test of symbolic name").getBytes())));
	final byte[] errorCode = { (byte) 0x25, (byte) 0x68, (byte) 0x95, (byte) 0x03 };
	assertEquals(tlvsToTest.get(7), new LSPUpdateErrorTlv(errorCode));
	final byte[] ipv4Address = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };
	final byte[] tunnelId1 = { (byte) 0x12, (byte) 0x34 };
	final byte[] extendedTunnelID1 = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };
	final byte[] lspId1 = { (byte) 0xFF, (byte) 0xFF };
	assertEquals(tlvsToTest.get(8), new IPv4LSPIdentifiersTlv(new IPv4Address(ipv4Address),
		new LSPIdentifier(lspId1), new TunnelIdentifier(tunnelId1), new IPv4ExtendedTunnelIdentifier(new IPv4Address(extendedTunnelID1))));
	final byte[] ipv6Address = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12,
		(byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 };
	final byte[] tunnelId2 = { (byte) 0xFF, (byte) 0xFF };
	final byte[] extendedTunnelID2 = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01,
		(byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67 };
	final byte[] lspId2 = { (byte) 0x12, (byte) 0x34 };
	assertEquals(tlvsToTest.get(9), new IPv6LSPIdentifiersTlv(new IPv6Address(ipv6Address),
		new LSPIdentifier(lspId2), new TunnelIdentifier(tunnelId2), new IPv6ExtendedTunnelIdentifier(new IPv6Address(extendedTunnelID2))));
	assertEquals(tlvsToTest.get(10), new RSVPErrorSpecTlv<IPv4Address>(new IPv4Address(ipv4Address), false, true, 0x92, 0x1602));
	assertEquals(tlvsToTest.get(11), new RSVPErrorSpecTlv<IPv6Address>(new IPv6Address(ipv6Address), true, false, 0xD5, 0xC5D9));
	assertEquals(tlvsToTest.get(12), new ReqMissingTlv(0xF7823517L));
	final byte[] valueBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	assertEquals(tlvsToTest.get(13), new NodeIdentifierTlv(valueBytes));
	assertEquals(tlvsToTest.get(14), new OrderTlv(0xFFFFFFFFL, 0x00000001L));
	assertEquals(tlvsToTest.get(15), new OFListTlv(new ArrayList<PCEPOFCodes>() {
	    private static final long serialVersionUID = 1L;

	    {
		this.add(PCEPOFCodes.MCC);
		this.add(PCEPOFCodes.MCP);
		this.add(PCEPOFCodes.MLL);
	    }
	}));
	assertEquals(tlvsToTest.get(16), new P2MPCapabilityTlv(2));

	assertArrayEquals(bytesFromFile, PCEPTlvParser.put(tlvsToTest));
    }

    @Test
    public void testDifferentLengthExceptions() {
	final byte[] bytes = { (byte) 0x00 }; // not empty but not enought data
					      // for parsing subobjects

	try {
	    LSPIdentifierIPv4TlvParser.parse(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    LSPIdentifierIPv6TlvParser.parse(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    PCEStatefulCapabilityTlvParser.deserializeValueField(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    RSVPErrorSpecIPv4TlvParser.parse(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    RSVPErrorSpecIPv6TlvParser.parse(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    OFListTlvParser.parse(bytes);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}
    }

    @Test
    public void testUnknownInstanceExceptions() {
	try {
	    LSPIdentifierIPv4TlvParser.put(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    LSPIdentifierIPv6TlvParser.put(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    PCEStatefulCapabilityTlvParser.serializeValueField(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    RSVPErrorSpecIPv4TlvParser.put(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    RSVPErrorSpecIPv6TlvParser.put(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    OFListTlvParser.put(null);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

    }

    @Test
    public void testEmptyExceptions() throws PCEPDeserializerException {
	final byte[] bytes = {}; // empty

	try {
	    LSPIdentifierIPv4TlvParser.parse(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    LSPIdentifierIPv6TlvParser.parse(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    PCEStatefulCapabilityTlvParser.deserializeValueField(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    RSVPErrorSpecIPv4TlvParser.parse(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    RSVPErrorSpecIPv6TlvParser.parse(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    OFListTlvParser.parse(bytes);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}
    }
}
