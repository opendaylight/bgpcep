/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;


public class APITest {

	@Test
	public void testDocumentedException() {
		final DocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
		assertEquals("Some message", de.getMessage());
		assertEquals(BGPError.BAD_BGP_ID, ((BGPDocumentedException) de).getError());
		assertNull(((BGPDocumentedException) de).getData());
	}

	@Test
	public void testBGPKeepAliveMessage() {
		final Notification msg = new KeepaliveBuilder().build();
		assertTrue(msg instanceof Keepalive);
	}

	@Test
	public void testBGPNotificationMessage() {
		final Notify msg = new NotifyBuilder().setErrorCode(BGPError.AS_PATH_MALFORMED.getCode()).setErrorSubcode(
				BGPError.AS_PATH_MALFORMED.getSubcode()).build();
		assertTrue(msg instanceof Notify);
		assertEquals(BGPError.AS_PATH_MALFORMED.getCode(), msg.getErrorCode().shortValue());
		assertEquals(BGPError.AS_PATH_MALFORMED.getSubcode(), msg.getErrorSubcode().shortValue());
		assertNull(msg.getData());
	}

	@Test
	public void testBGPOpenMessage() {
		final Notification msg = new OpenBuilder().setMyAsNumber(58).setHoldTimer(5).build();
		assertNull(((Open) msg).getBgpParameters());
	}

	@Test
	public void testToString() {
		final Notification o = new OpenBuilder().setMyAsNumber(58).setHoldTimer(5).build();
		final Notification n = new NotifyBuilder().setErrorCode(BGPError.AS_PATH_MALFORMED.getCode()).setErrorSubcode(
				BGPError.AS_PATH_MALFORMED.getSubcode()).build();
		assertNotSame(o.toString(), n.toString());
	}
}
