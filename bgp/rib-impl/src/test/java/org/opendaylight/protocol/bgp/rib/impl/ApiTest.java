/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPUpdateSynchronized;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.AS4BytesCapability;
import org.opendaylight.protocol.bgp.parser.parameter.CapabilityParameter;
import org.opendaylight.protocol.bgp.parser.parameter.GracefulCapability;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.bgp.rib.impl.BGPUpdateSynchronizedImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;

import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.concepts.ASNumber;
import com.google.common.collect.Maps;

public class ApiTest {

	@Test
	public void testDocumentedException() {
		final DocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
		assertEquals("Some message", de.getMessage());
		assertEquals(BGPError.BAD_BGP_ID, ((BGPDocumentedException) de).getError());
		assertNull(((BGPDocumentedException) de).getData());
	}

	@Test
	public void testBGPKeepAliveMessage() {
		final BGPMessage msg = new BGPKeepAliveMessage();
		assertTrue(msg instanceof BGPKeepAliveMessage);
	}

	@Test
	public void testBGPNotificationMessage() {
		final BGPMessage msg = new BGPNotificationMessage(BGPError.AS_PATH_MALFORMED);
		assertTrue(msg instanceof BGPNotificationMessage);
		assertEquals(BGPError.AS_PATH_MALFORMED, ((BGPNotificationMessage) msg).getError());
		assertNull(((BGPNotificationMessage) msg).getData());
	}

	@Test
	public void testBGPOpenMessage() {
		final BGPMessage msg = new BGPOpenMessage(new ASNumber(58), (short) 5, null, null);
		assertNull(((BGPOpenMessage) msg).getOptParams());
	}

	@Test
	public void testBGPParameter() {

		final BGPTableType t = new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Unicast);
		final BGPTableType t1 = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);

		final BGPParameter tlv1 = new MultiprotocolCapability(t);

		final BGPParameter tlv2 = new MultiprotocolCapability(t1);

		final Map<BGPTableType, Boolean> tt = Maps.newHashMap();
		tt.put(t, true);
		tt.put(t1, false);

		final BGPParameter tlv3 = new GracefulCapability(false, 0, tt);

		final BGPParameter tlv4 = new AS4BytesCapability(new ASNumber(40));

		assertFalse(((GracefulCapability) tlv3).isRestartFlag());

		assertEquals(0, ((GracefulCapability) tlv3).getRestartTimerValue());

		assertEquals(tlv1.getType(), tlv2.getType());

		assertFalse(tlv1.equals(tlv2));

		assertNotSame(tlv1.hashCode(), tlv3.hashCode());

		assertNotSame(tlv2.toString(), tlv3.toString());

		assertEquals(((GracefulCapability) tlv3).getTableTypes(), tt);

		assertNotSame(((CapabilityParameter) tlv1).getCode(), ((CapabilityParameter) tlv3).getCode());

		assertEquals(((MultiprotocolCapability) tlv1).getSafi(), ((MultiprotocolCapability) tlv2).getSafi());

		assertNotSame(((MultiprotocolCapability) tlv1).getAfi(), ((MultiprotocolCapability) tlv2).getAfi());

		assertEquals(40, ((AS4BytesCapability) tlv4).getASNumber().getAsn());

		assertEquals(new AS4BytesCapability(new ASNumber(40)).toString(), tlv4.toString());
	}

	@Test
	public void testToString() {
		final BGPMessage o = new BGPOpenMessage(new ASNumber(58), (short) 5, null, null);
		final BGPMessage n = new BGPNotificationMessage(BGPError.ATTR_FLAGS_MISSING);
		assertNotSame(o.toString(), n.toString());
	}

	@Test
	public void testBGPSessionPreferences() {
		final SessionPreferences sp = new BGPSessionPreferences(new ASNumber(58), (short) 5, null, null);
		assertNull(((BGPSessionPreferences) sp).getBgpId());
		assertEquals((short) 5, ((BGPSessionPreferences) sp).getHoldTime());
		assertEquals(58, ((BGPSessionPreferences) sp).getMyAs().getAsn());
	}

	@Test
	public void testBGPUpdateSynchronized() {
		final BGPTableType tt = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Linkstate);
		final BGPUpdateSynchronized update = new BGPUpdateSynchronizedImpl(tt);
		assertEquals(tt, update.getTableType());
	}
}
