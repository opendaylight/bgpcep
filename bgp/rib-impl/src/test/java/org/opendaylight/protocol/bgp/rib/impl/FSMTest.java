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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;

import org.opendaylight.protocol.concepts.ASNumber;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FSMTest {

	private SimpleSessionListener clientListener;

	private final SpeakerSessionListener speakerListener = new SpeakerSessionListener();

	private SpeakerSessionMock speaker;

	@Before
	public void setUp() {
		this.clientListener = new SimpleSessionListener();
		this.speaker = new SpeakerSessionMock(this.speakerListener, this.clientListener);
		this.clientListener.addSession(this.speaker);
	}

	@Test
	public void testAccSessionChar() throws InterruptedException {
		this.speaker.startSession();
		assertEquals(1, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(0) instanceof BGPOpenMessage);
		final List<BGPParameter> tlvs = Lists.newArrayList();
		tlvs.add(new MultiprotocolCapability(new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast)));
		this.clientListener.sendMessage(new BGPOpenMessage(new ASNumber(30), (short) 3, null, tlvs));
		assertEquals(2, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(1) instanceof BGPKeepAliveMessage);
		this.clientListener.sendMessage(new BGPKeepAliveMessage());
		synchronized (this.speakerListener) {
			while (!this.speakerListener.up)
				try {
					this.speakerListener.wait();
					fail("Exception should have occured.");
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
		}
		assertTrue(this.speakerListener.up);
		assertEquals(this.speakerListener.types,
				Sets.newHashSet(new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast)));
		Thread.sleep(1 * 1000);
		assertEquals(3, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(2) instanceof BGPKeepAliveMessage); // test of keepalive timer
		this.clientListener.sendMessage(new BGPOpenMessage(new ASNumber(30), (short) 3, null, null));
		assertEquals(4, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(3) instanceof BGPNotificationMessage);
		final BGPMessage m = this.clientListener.getListMsg().get(3);
		assertEquals(BGPError.FSM_ERROR, ((BGPNotificationMessage) m).getError());
	}

	@Test
	public void testNotAccChars() throws InterruptedException {
		this.speaker.startSession();
		assertEquals(1, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(0) instanceof BGPOpenMessage);
		this.clientListener.sendMessage(new BGPOpenMessage(new ASNumber(30), (short) 1, null, null));
		assertEquals(2, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(1) instanceof BGPKeepAliveMessage);
		assertFalse(this.speakerListener.up);
		Thread.sleep(BGPSessionImpl.HOLD_TIMER_VALUE * 1000);
		Thread.sleep(100);
		final BGPMessage m = this.clientListener.getListMsg().get(this.clientListener.getListMsg().size() - 1);
		assertEquals(BGPError.HOLD_TIMER_EXPIRED, ((BGPNotificationMessage) m).getError());
	}

	@Test
	@Ignore
	// long duration
	public void testNoOpen() throws InterruptedException {
		this.speaker.startSession();
		assertEquals(1, this.clientListener.getListMsg().size());
		assertTrue(this.clientListener.getListMsg().get(0) instanceof BGPOpenMessage);
		Thread.sleep(BGPSessionImpl.HOLD_TIMER_VALUE * 1000);
		Thread.sleep(100);
		final BGPMessage m = this.clientListener.getListMsg().get(this.clientListener.getListMsg().size() - 1);
		assertEquals(BGPError.HOLD_TIMER_EXPIRED, ((BGPNotificationMessage) m).getError());
	}

	@Test
	public void sendNotification() {
		this.speaker.startSession();
		this.clientListener.sendMessage(new BGPOpenMessage(new ASNumber(30), (short) 3, null, null));
		this.clientListener.sendMessage(new BGPKeepAliveMessage());
		synchronized (this.speakerListener) {
			while (!this.speakerListener.up)
				try {
					this.speakerListener.wait();
					fail("Exception should have occured.");
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
		}
		assertTrue(this.speakerListener.up);
		this.clientListener.sendMessage(new BGPNotificationMessage(BGPError.CEASE));
		assertFalse(this.speakerListener.up);
	}

	@Test
	public void complementaryTests() {
		assertEquals(4096, this.speaker.maximumMessageSize());

		assertNotNull(this.speaker.getStream());
	}

	@After
	public void tearDown() {
		this.speaker.close();
	}
}
