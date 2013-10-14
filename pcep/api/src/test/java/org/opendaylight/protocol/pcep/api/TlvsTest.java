/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.concepts.IPv4ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.IPv6ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.IPv4LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.IPv6LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.LSPCleanupTlv;
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.pcep.tlv.LSPSymbolicNameTlv;
import org.opendaylight.protocol.pcep.tlv.LSPUpdateErrorTlv;
import org.opendaylight.protocol.pcep.tlv.NoPathVectorTlv;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;
import org.opendaylight.protocol.pcep.tlv.OFListTlv;
import org.opendaylight.protocol.pcep.tlv.OverloadedDurationTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv;
import org.opendaylight.protocol.pcep.tlv.ReqMissingTlv;

/**
 *
 */
public class TlvsTest {
	@Test
	public void IPv4LSPIdentifiersTlvTest() {
		final IPv4LSPIdentifiersTlv m = new IPv4LSPIdentifiersTlv(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(
				new byte[] { (byte) 127, (byte) 0 }), new IPv4ExtendedTunnelIdentifier(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 })));
		final IPv4LSPIdentifiersTlv m2 = new IPv4LSPIdentifiersTlv(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(
				new byte[] { (byte) 127, (byte) 0 }), new IPv4ExtendedTunnelIdentifier(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 })));
		final IPv4LSPIdentifiersTlv m3 = new IPv4LSPIdentifiersTlv(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(
				new byte[] { (byte) 127, (byte) 0 }), new IPv4ExtendedTunnelIdentifier(new IPv4Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 3 })));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getExtendedTunnelID(), m2.getExtendedTunnelID());
		assertEquals(m.getLspID(), m2.getLspID());
		assertEquals(m.getSenderAddress(), m2.getSenderAddress());
		assertEquals(m.getTunnelID(), m2.getTunnelID());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void IPv6LSPIdentifiersTlvTest() {
		final IPv6LSPIdentifiersTlv m = new IPv6LSPIdentifiersTlv(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(new byte[] {
				(byte) 127, (byte) 0 }), new IPv6ExtendedTunnelIdentifier(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 2 })));
		final IPv6LSPIdentifiersTlv m2 = new IPv6LSPIdentifiersTlv(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(new byte[] {
				(byte) 127, (byte) 0 }), new IPv6ExtendedTunnelIdentifier(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 2 })));
		final IPv6LSPIdentifiersTlv m3 = new IPv6LSPIdentifiersTlv(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 2 }), new LSPIdentifier(new byte[] { (byte) 127, (byte) 0 }), new TunnelIdentifier(new byte[] {
				(byte) 127, (byte) 0 }), new IPv6ExtendedTunnelIdentifier(new IPv6Address(
				new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2, (byte) 127, (byte) 0, (byte) 0, (byte) 2,
						(byte) 127, (byte) 0, (byte) 0, (byte) 3 })));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getExtendedTunnelID(), m2.getExtendedTunnelID());
		assertEquals(m.getLspID(), m2.getLspID());
		assertEquals(m.getSenderAddress(), m2.getSenderAddress());
		assertEquals(m.getTunnelID(), m2.getTunnelID());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void LSPStateDBVersionTlvTest() {
		final LSPStateDBVersionTlv m = new LSPStateDBVersionTlv(25);
		final LSPStateDBVersionTlv m2 = new LSPStateDBVersionTlv(25);
		final LSPStateDBVersionTlv m3 = new LSPStateDBVersionTlv(26);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getDbVersion(), m2.getDbVersion());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void LSPSymbolicNameTlvTest() {
		final LSPSymbolicNameTlv m = new LSPSymbolicNameTlv(new LSPSymbolicName(new byte[] { (byte) 2 }));
		final LSPSymbolicNameTlv m2 = new LSPSymbolicNameTlv(new LSPSymbolicName(new byte[] { (byte) 2 }));
		final LSPSymbolicNameTlv m3 = new LSPSymbolicNameTlv(new LSPSymbolicName(new byte[] { (byte) 3 }));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getSymbolicName(), m2.getSymbolicName());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void LSPUpdateErrorTlvTest() {
		final LSPUpdateErrorTlv m = new LSPUpdateErrorTlv(new byte[] { (byte) 2, (byte) 2, (byte) 2, (byte) 2 });
		final LSPUpdateErrorTlv m2 = new LSPUpdateErrorTlv(new byte[] { (byte) 2, (byte) 2, (byte) 2, (byte) 2 });
		final LSPUpdateErrorTlv m3 = new LSPUpdateErrorTlv(new byte[] { (byte) 2, (byte) 2, (byte) 2, (byte) 3 });

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertArrayEquals(m.getErrorCode(), m2.getErrorCode());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void NoPathVectorTlvTest() {
		final NoPathVectorTlv m = new NoPathVectorTlv(false, true, false, false, false, false);
		final NoPathVectorTlv m2 = new NoPathVectorTlv(false, true, false, false, false, false);
		final NoPathVectorTlv m3 = new NoPathVectorTlv(false, true, true, false, false, false);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertFalse(m.isPceUnavailable());
		assertTrue(m.isUnknownDest());
		assertFalse(m.isUnknownSrc());
		assertFalse(m.isNoGCOSolution());
		assertFalse(m.isNoGCOMigrationPath());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void NodeIdentifierTlvTest() throws UnsupportedEncodingException {
		final NodeIdentifierTlv m = new NodeIdentifierTlv(new byte[] { (byte) 2 });
		final NodeIdentifierTlv m2 = new NodeIdentifierTlv(new byte[] { (byte) 2 });
		final NodeIdentifierTlv m3 = new NodeIdentifierTlv(new byte[] { (byte) 79, (byte) 102, (byte) 45, (byte) 57, (byte) 107, (byte) 45, (byte) 48,
				(byte) 50 });

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertArrayEquals(m.getValue(), m2.getValue());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
		assertEquals(m3.toString(), "NodeIdentifierTlv [value=Of-9k-02]");

		//non-ascii string
		final String str = "þščťžéíá";

		//test utf-8 validity
		assertEquals("NodeIdentifierTlv [value=" + str + "]", new NodeIdentifierTlv(str.getBytes("UTF-8")).toString());

		//test enother encoding, should be represented as raw binary
		assertEquals("NodeIdentifierTlv [value=" + Arrays.toString(str.getBytes("ISO8859_1")) + "]",
				new NodeIdentifierTlv(str.getBytes("ISO8859_1")).toString());
	}

	@Test
	public void OverloadedDurationTlvTest() {
		final OverloadedDurationTlv m = new OverloadedDurationTlv(2);
		final OverloadedDurationTlv m2 = new OverloadedDurationTlv(2);
		final OverloadedDurationTlv m3 = new OverloadedDurationTlv(3);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getValue(), m2.getValue());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void PCEStatefulCapabilityTlvTest() {
		final PCEStatefulCapabilityTlv m = new PCEStatefulCapabilityTlv(false, true, false);
		final PCEStatefulCapabilityTlv m2 = new PCEStatefulCapabilityTlv(false, true, false);
		final PCEStatefulCapabilityTlv m3 = new PCEStatefulCapabilityTlv(true, true, true);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void RSVPErrorSpecTlvTest() {
		final RSVPErrorSpecTlv<IPv4Address> m = new RSVPErrorSpecTlv<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), true, true, 2, 2);
		final RSVPErrorSpecTlv<IPv4Address> m2 = new RSVPErrorSpecTlv<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), true, true, 2, 2);
		final RSVPErrorSpecTlv<IPv4Address> m3 = new RSVPErrorSpecTlv<IPv4Address>(new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }), false, true, 2, 2);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getErrorCode(), m2.getErrorCode());
		assertEquals(m.getErrorNodeAddress(), m2.getErrorNodeAddress());
		assertEquals(m.getErrorValue(), m2.getErrorValue());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void ReqMissingTlvTest() {
		final ReqMissingTlv m = new ReqMissingTlv(2);
		final ReqMissingTlv m2 = new ReqMissingTlv(2);
		final ReqMissingTlv m3 = new ReqMissingTlv(3);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getRequestID(), m2.getRequestID());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void OFListTlvTest() {
		final List<PCEPOFCodes> ofcodes = new ArrayList<PCEPOFCodes>();

		final OFListTlv m = new OFListTlv(ofcodes);
		final OFListTlv m2 = new OFListTlv(ofcodes);
		final OFListTlv m3 = new OFListTlv(null);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getOfCodes(), m2.getOfCodes());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));
	}

	@Test
	public void LSPCleanupTlvTest() {
		final LSPCleanupTlv m = new LSPCleanupTlv(Integer.MAX_VALUE);
		final LSPCleanupTlv m2 = new LSPCleanupTlv(Integer.MAX_VALUE);
		final LSPCleanupTlv m3 = new LSPCleanupTlv(0);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getTimeout(), m2.getTimeout());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		try {
			new LSPCleanupTlv(Integer.MIN_VALUE);
		} catch (final IllegalArgumentException e) {
			assertEquals("Timeout (" + Integer.MIN_VALUE + ") cannot be negative or bigger than 2^31 -1.", e.getMessage());
		}
	}
}
