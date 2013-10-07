/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPNotificationMessage;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;

public class FiniteStateMachineTest {

	private ServerSessionMock serverSession;

	private final SimpleSessionListener serverListener = new SimpleSessionListener();

	private MockPCE client;

	@Before
	public void setUp() {
		this.client = new MockPCE();
		this.serverSession = new ServerSessionMock(this.serverListener, this.client);
		this.client.addSession(this.serverSession);
	}

	/**
	 * Both PCEs accept session characteristics. Also tests KeepAliveTimer and error message and when pce attempts to
	 * establish pce session for the 2nd time.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	@Ignore
	public void testSessionCharsAccBoth() throws InterruptedException {
		// this.serverSession.startSession();
		assertEquals(1, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(0) instanceof PCEPOpenMessage);
		this.client.sendMessage(new PCEPOpenMessage(new PCEPOpenObject(3, 9, 2)));
		assertEquals(2, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(1) instanceof KeepaliveMessage);
		this.client.sendMessage((Message) new KeepaliveMessageBuilder().build());
		synchronized (this.serverListener) {
			while (!this.serverListener.up) {
				try {
					this.serverListener.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		assertTrue(this.serverListener.up);
		// Thread.sleep(PCEPSessionImpl.KEEP_ALIVE_TIMER_VALUE * 1000);
		// assertEquals(3, this.client.getListMsg().size());
		// assertTrue(this.client.getListMsg().get(2) instanceof PCEPKeepAliveMessage); // test of keepalive timer
		this.client.sendMessage(new PCEPOpenMessage(new PCEPOpenObject(1, 1, 1)));
		assertEquals(3, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(2) instanceof PCEPErrorMessage);
		for (final Message m : this.client.getListMsg()) {
			if (m instanceof PCEPErrorMessage) {
				final PCEPErrorObject obj = ((PCEPErrorMessage) m).getErrorObjects().get(0);
				assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, obj.getError()); // test of error type 9
			}
		}
	}

	/**
	 * Mock PCE does not accept session characteristics the first time.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	@Ignore
	public void testSessionCharsAccMe() throws InterruptedException {
		// this.serverSession.startSession();
		this.client.sendMessage(new PCEPOpenMessage(new PCEPOpenObject(4, 9, 2)));
		assertEquals(2, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(0) instanceof PCEPOpenMessage);
		assertTrue(this.client.getListMsg().get(1) instanceof KeepaliveMessage);
		this.client.sendErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR, new PCEPOpenObject(3, 7, 2, null));
		assertEquals(3, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(2) instanceof PCEPOpenMessage);
		this.client.sendMessage((Message) new KeepaliveMessageBuilder().build());
		synchronized (this.serverListener) {
			while (!this.serverListener.up) {
				try {
					this.serverListener.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		assertTrue(this.serverListener.up);
	}

	/**
	 * Sending different PCEP Message than Open in session establishment phase.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	@Ignore
	public void testErrorOneOne() throws InterruptedException {
		// this.serverSession.startSession();
		assertEquals(1, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(0) instanceof PCEPOpenMessage);
		this.client.sendMessage(new PCEPNotificationMessage(new ArrayList<CompositeNotifyObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeNotifyObject(new ArrayList<PCEPNotificationObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPNotificationObject((short) 1, (short) 1));
					}
				}));
			}
		}));
		for (final Message m : this.client.getListMsg()) {
			if (m instanceof PCEPErrorMessage) {
				final PCEPErrorObject obj = ((PCEPErrorMessage) m).getErrorObjects().get(0);
				assertEquals(PCEPErrors.NON_OR_INVALID_OPEN_MSG, obj.getError());
			}
		}
	}

	/************* Tests commented because of their long duration (tested timers) **************/

	/**
	 * OpenWait timer expired.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	@Ignore
	public void testErrorOneTwo() throws InterruptedException {
		// this.serverSession.startSession();
		assertEquals(1, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(0) instanceof PCEPOpenMessage);
		Thread.sleep(60 * 1000);
		for (final Message m : this.client.getListMsg()) {
			if (m instanceof PCEPErrorMessage) {
				final PCEPErrorObject obj = ((PCEPErrorMessage) m).getErrorObjects().get(0);
				assertEquals(PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT, obj.getError());
			}
		}
	}

	/**
	 * KeepWaitTimer expired.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	@Ignore
	public void testErrorOneSeven() throws InterruptedException {
		// this.serverSession.startSession();
		assertEquals(1, this.client.getListMsg().size());
		assertTrue(this.client.getListMsg().get(0) instanceof PCEPOpenMessage);
		this.client.sendMessage(new PCEPOpenMessage(new PCEPOpenObject(3, 9, 2)));
		Thread.sleep(this.serverSession.getKeepAliveTimerValue() * 1000);
		for (final Message m : this.client.getListMsg()) {
			if (m instanceof PCEPErrorMessage) {
				final PCEPErrorObject obj = ((PCEPErrorMessage) m).getErrorObjects().get(0);
				assertEquals(PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT, obj.getError());
			}
		}
	}

	@Test
	@Ignore
	public void testUnknownMessage() throws InterruptedException {
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(1, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(10000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(2, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(10000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(20000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(4, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(30000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(10000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(5000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(4, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(1000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		assertEquals(5, this.serverSession.unknownMessagesTimes.size());
		Thread.sleep(1000);
		this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		synchronized (this.client) {
			while (!this.client.down) {
				try {
					this.client.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		assertTrue(this.client.down);
	}

	@After
	public void tearDown() {
		this.serverSession.close();
	}
}
