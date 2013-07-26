/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.GracefulCapability;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.util.ByteArray;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ParserTest {

	public static final byte[] openBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4,
			(byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00 };

	public static final byte[] keepAliveBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04 };

	public static final byte[] notificationBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x17, (byte) 0x03, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x09 };

	final ProtocolMessageFactory factory = new BGPMessageFactory();

	@Test
	public void testHeaderErrors() throws DeserializerException, DocumentedException {
		byte[] wrong = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00 };
		wrong = ByteArray.cutBytes(wrong, 16);
		try {
			this.factory.parse(wrong);
			fail("Exception should have occcured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Too few bytes in passed array. Passed: " + wrong.length + ". Expected: >= "
					+ BGPMessageFactory.COMMON_HEADER_LENGTH + ".", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void testBadMsgType() throws DeserializerException {
		byte[] bytes = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x13, (byte) 0x08 };
		bytes = ByteArray.cutBytes(bytes, 16);
		try {
			this.factory.parse(bytes);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertEquals(BGPError.BAD_MSG_TYPE, ((BGPDocumentedException) e).getError());
			return;
		}
		fail();
	}

	@Test
	public void testKeepAliveMsg() throws DeserializerException, DocumentedException {
		final BGPMessage keepAlive = new BGPKeepAliveMessage();
		byte[] bytes = this.factory.put(keepAlive);
		assertArrayEquals(keepAliveBMsg, bytes);

		bytes = ByteArray.cutBytes(bytes, 16);
		final BGPMessage m = (BGPMessage) this.factory.parse(bytes);

		assertTrue(m instanceof BGPKeepAliveMessage);
	}

	@Test
	public void testBadKeepAliveMsg() throws DeserializerException {
		byte[] bytes = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x14, (byte) 0x04, (byte) 0x05 };

		bytes = ByteArray.cutBytes(bytes, 16);
		try {
			this.factory.parse(bytes);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertThat(e.getMessage(), containsString("Message length field not within valid range."));
			assertEquals(BGPError.BAD_MSG_LENGTH, ((BGPDocumentedException) e).getError());
			return;
		}
		fail();
	}

	@Test
	public void testOpenMessage() throws UnknownHostException, DeserializerException, DocumentedException {
		final BGPMessage open = new BGPOpenMessage(new ASNumber(100), (short) 180, new IPv4Address(InetAddress.getByName("20.20.20.20")), null);
		byte[] bytes = this.factory.put(open);
		assertArrayEquals(openBMsg, bytes);

		bytes = ByteArray.cutBytes(bytes, 16);
		final BGPMessage m = (BGPMessage) this.factory.parse(bytes);

		assertTrue(m instanceof BGPOpenMessage);
		assertEquals(new ASNumber(100), ((BGPOpenMessage) m).getMyAS());
		assertEquals((short) 180, ((BGPOpenMessage) m).getHoldTime());
		assertEquals(new IPv4Address(InetAddress.getByName("20.20.20.20")), ((BGPOpenMessage) m).getBgpId());
		assertTrue(((BGPOpenMessage) m).getOptParams().isEmpty());
	}

	@Test
	public void testBadHoldTimeError() throws DeserializerException {
		byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x01, (byte) 0x14, (byte) 0x14,
				(byte) 0x14, (byte) 0x14, (byte) 0x00 };

		bMsg = ByteArray.cutBytes(bMsg, 16);
		try {
			this.factory.parse(bMsg);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertEquals("Hold time value not acceptable.", e.getMessage());
			assertEquals(BGPError.HOLD_TIME_NOT_ACC, ((BGPDocumentedException) e).getError());
			return;
		}
		fail();
	}

	@Test
	public void testBadMsgLength() throws DeserializerException {
		byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x1b, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4, (byte) 0xff, (byte) 0xff,
				(byte) 0xff };

		bMsg = ByteArray.cutBytes(bMsg, 16);
		try {
			this.factory.parse(bMsg);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertEquals("Open message too small.", e.getMessage());
			assertEquals(BGPError.BAD_MSG_LENGTH, ((BGPDocumentedException) e).getError());
		}
	}

	@Test
	public void testBadVersion() throws DeserializerException {
		byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x1d, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4, (byte) 0x14, (byte) 0x14,
				(byte) 0x14, (byte) 0x14, (byte) 0x00 };

		bMsg = ByteArray.cutBytes(bMsg, 16);
		try {
			this.factory.parse(bMsg);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertEquals("BGP Protocol version 8 not supported.", e.getMessage());
			assertEquals(BGPError.VERSION_NOT_SUPPORTED, ((BGPDocumentedException) e).getError());
			return;
		}
		fail();
	}

	@Test
	public void testNotificationMsg() throws DeserializerException, DocumentedException {
		BGPMessage notMsg = new BGPNotificationMessage(BGPError.OPT_PARAM_NOT_SUPPORTED, new byte[] { 4, 9 });
		byte[] bytes = this.factory.put(notMsg);
		assertArrayEquals(notificationBMsg, bytes);

		bytes = ByteArray.cutBytes(bytes, 16);
		BGPMessage m = (BGPMessage) this.factory.parse(bytes);

		assertTrue(m instanceof BGPNotificationMessage);
		assertEquals(BGPError.OPT_PARAM_NOT_SUPPORTED, ((BGPNotificationMessage) m).getError());
		assertArrayEquals(new byte[] { 4, 9 }, ((BGPNotificationMessage) m).getData());

		notMsg = new BGPNotificationMessage(BGPError.CONNECTION_NOT_SYNC);
		bytes = this.factory.put(notMsg);

		bytes = ByteArray.cutBytes(bytes, 16);
		m = (BGPMessage) this.factory.parse(bytes);

		assertTrue(m instanceof BGPNotificationMessage);
		assertEquals(BGPError.CONNECTION_NOT_SYNC, ((BGPNotificationMessage) m).getError());
		assertNull(((BGPNotificationMessage) m).getData());
	}

	@Test
	public void testWrongLength() throws DeserializerException {
		byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x14, (byte) 0x03, (byte) 0x02 };

		bMsg = ByteArray.cutBytes(bMsg, 16);
		try {
			this.factory.parse(bMsg);
			fail("Exception should have occured.");
		} catch (final DocumentedException e) {
			assertEquals("Notification message too small.", e.getMessage());
			assertEquals(BGPError.BAD_MSG_LENGTH, ((BGPDocumentedException) e).getError());
			return;
		}
		fail();
	}

	@Test
	public void testUnrecognizedError() throws DeserializerException, DocumentedException {
		byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00,
				(byte) 0x15, (byte) 0x03, (byte) 0x02, (byte) 0xaa };

		bMsg = ByteArray.cutBytes(bMsg, 16);
		try {
			this.factory.parse(bMsg);
			fail("Exception should have occured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("BGP Error code 2 and subcode 170 not recognized.", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void testTLVParser() throws UnknownHostException {

		final BGPTableType t1 = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);
		final BGPTableType t2 = new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Unicast);

		final List<BGPParameter> tlvs = Lists.newArrayList();
		tlvs.add(new MultiprotocolCapability(t1));
		tlvs.add(new MultiprotocolCapability(t2));
		final Map<BGPTableType, Boolean> tableTypes = Maps.newHashMap();
		tableTypes.put(t1, true);
		tableTypes.put(t2, true);
		tlvs.add(new GracefulCapability(true, 0, tableTypes));
		final BGPOpenMessage open = new BGPOpenMessage(new ASNumber(72), (short) 180, new IPv4Address(InetAddress.getByName("172.20.160.170")), tlvs);

		// System.out.println(Arrays.toString(this.factory.put(open)));

		this.factory.put(open);

		// assertArrayEquals(openWithCpblt, bytes);
	}
}
