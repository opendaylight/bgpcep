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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.pcep.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Lists;

public class FiniteStateMachineTest {

	private DefaultPCEPSessionNegotiator serverSession;

	@Mock
	private Channel clientListener;

	@Mock
	private ChannelPipeline pipeline;

	private final List<Notification> receivedMsgs = Lists.newArrayList();

	private Open openmsg;

	private Keepalive kamsg;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open localPrefs = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder().setKeepalive(
				(short) 1).build();
		this.serverSession = new DefaultPCEPSessionNegotiator(new HashedWheelTimer(), new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE), this.clientListener, new SimpleSessionListener(), (short) 1, 20, localPrefs);
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) {
				final Object[] args = invocation.getArguments();
				FiniteStateMachineTest.this.receivedMsgs.add((Notification) args[0]);
				return null;
			}
		}).when(this.clientListener).writeAndFlush(any(Notification.class));
		doReturn("TestingChannel").when(this.clientListener).toString();
		doReturn(this.pipeline).when(this.clientListener).pipeline();
		doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
		doReturn(mock(ChannelFuture.class)).when(this.clientListener).close();
		this.openmsg = new OpenBuilder().setOpenMessage(
				new OpenMessageBuilder().setOpen(
						new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder().setDeadTimer(
								(short) 3).build()).build()).build();
		this.kamsg = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();
	}

	/**
	 * Both PCEs accept session characteristics. Also tests KeepAliveTimer and error message and when pce attempts to
	 * establish pce session for the 2nd time.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSessionCharsAccBoth() throws Exception {
		this.serverSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.serverSession.handleMessage(this.openmsg);
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof Keepalive);
		this.serverSession.handleMessage(this.kamsg);
		assertEquals(this.serverSession.getState(), DefaultPCEPSessionNegotiator.State.Finished);
		// Thread.sleep(PCEPSessionImpl.KEEP_ALIVE_TIMER_VALUE * 1000);
		// assertEquals(3, this.client.getListMsg().size());
		// assertTrue(this.client.getListMsg().get(2) instanceof PCEPKeepAliveMessage); // test of keepalive timer
		// this.client.sendMessage(new PCEPOpenMessage(new PCEPOpenObject(1, 1, 1)));
		// assertEquals(3, this.client.getListMsg().size());
		// assertTrue(this.client.getListMsg().get(2) instanceof PCEPErrorMessage);
		// for (final Message m : this.client.getListMsg()) {
		// if (m instanceof PCEPErrorMessage) {
		// final PCEPErrorObject obj = ((PCEPErrorMessage) m).getErrorObjects().get(0);
		// assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, obj.getError()); // test of error type 9
		// }
		// }
	}

	/**
	 * Mock PCE does not accept session characteristics the first time.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSessionCharsAccMe() throws Exception {
		this.serverSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.serverSession.handleMessage(this.openmsg);
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof Keepalive);
		this.serverSession.handleMessage(createErrorMessageWOpen(PCEPErrors.NON_ACC_NEG_SESSION_CHAR));
		assertEquals(3, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(2) instanceof Open);
		this.serverSession.handleMessage(this.kamsg);
		assertEquals(this.serverSession.getState(), DefaultPCEPSessionNegotiator.State.Finished);
	}

	private Pcerr createErrorMessageWOpen(final PCEPErrors e) {
		final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
		return new PcerrBuilder().setPcerrMessage(
				new PcerrMessageBuilder().setErrorType(
						new SessionBuilder().setOpen(
								new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder().setKeepalive(
										(short) 1).build()).build()).setErrors(
						Arrays.asList(new ErrorsBuilder().setErrorObject(
								new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e).type).setValue(
										maping.getFromErrorsEnum(e).value).build()).build())).build()).build();
	}

	/**
	 * Sending different PCEP Message than Open in session establishment phase.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testErrorOneOne() throws Exception {
		this.serverSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.serverSession.handleMessage(this.kamsg);
		for (final Notification m : this.receivedMsgs) {
			if (m instanceof Pcerr) {
				final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
				assertEquals(new Short((short) 1), obj.getErrorObject().getType());
				assertEquals(new Short((short) 1), obj.getErrorObject().getValue());
			}
		}
	}

	/**
	 * KeepWaitTimer expired.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testErrorOneSeven() throws Exception {
		this.serverSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.serverSession.handleMessage(this.openmsg);
		Thread.sleep(1000);
		for (final Notification m : this.receivedMsgs) {
			if (m instanceof Pcerr) {
				final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
				assertEquals(new Short((short) 1), obj.getErrorObject().getType());
				assertEquals(new Short((short) 7), obj.getErrorObject().getValue());
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
		this.serverSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof OpenMessage);
		Thread.sleep(60 * 1000);
		for (final Notification m : this.receivedMsgs) {
			if (m instanceof Pcerr) {
				final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
				assertEquals(new Short((short) 1), obj.getErrorObject().getType());
				assertEquals(new Short((short) 2), obj.getErrorObject().getValue());
			}
		}
	}

	@Test
	@Ignore
	public void testUnknownMessage() throws InterruptedException {
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(1, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(10000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(2, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(10000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(20000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(4, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(30000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(10000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(3, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(5000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(4, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(1000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// assertEquals(5, this.serverSession.unknownMessagesTimes.size());
		// Thread.sleep(1000);
		// this.serverSession.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
		// synchronized (this.client) {
		// while (!this.client.down) {
		// try {
		// this.client.wait();
		// } catch (final InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// assertTrue(this.client.down);
	}

	@After
	public void tearDown() {
	}
}
