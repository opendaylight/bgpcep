/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.concepts.ASNumber;

import com.google.common.collect.Lists;

public class FSMTest {

	private BGPSessionNegotiator clientSession;

	@Mock
	private Channel speakerListener;

	@Mock
	private ChannelPipeline pipeline;

	private final BGPTableType ipv4tt = new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);

	private final BGPTableType linkstatett = new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Linkstate);

	private final List<BGPMessage> receivedMsgs = Lists.newArrayList();

	private BGPOpenMessage classicOpen;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		final List<BGPParameter> tlvs = Lists.newArrayList();
		tlvs.add(new MultiprotocolCapability(this.ipv4tt));
		tlvs.add(new MultiprotocolCapability(this.linkstatett));
		final BGPSessionPreferences prefs = new BGPSessionPreferences(new ASNumber(30), (short) 3, null, tlvs);
		this.clientSession = new BGPSessionNegotiator(new HashedWheelTimer(), new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE), this.speakerListener, prefs, new SimpleSessionListener());
		doAnswer(new Answer() {
			@Override
			public Object answer(final InvocationOnMock invocation) {
				final Object[] args = invocation.getArguments();
				FSMTest.this.receivedMsgs.add((BGPMessage) args[0]);
				return null;
			}
		}).when(this.speakerListener).writeAndFlush(any(BGPMessage.class));
		doReturn("TestingChannel").when(this.speakerListener).toString();
		doReturn(this.pipeline).when(this.speakerListener).pipeline();
		doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
		doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();
		this.classicOpen = new BGPOpenMessage(new ASNumber(30), (short) 3, null, tlvs);
	}

	@Test
	public void testAccSessionChar() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof BGPOpenMessage);
		this.clientSession.handleMessage(this.classicOpen);
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof BGPKeepAliveMessage);
		this.clientSession.handleMessage(new BGPKeepAliveMessage());
		assertEquals(this.clientSession.getState(), BGPSessionNegotiator.State.Finished);
		// Thread.sleep(3 * 1000);
		// Thread.sleep(100);
		// assertEquals(3, this.receivedMsgs.size());
		// assertTrue(this.receivedMsgs.get(2) instanceof BGPKeepAliveMessage); // test of keepalive timer
		// this.clientSession.handleMessage(new BGPOpenMessage(new ASNumber(30), (short) 3, null, null));
		// assertEquals(4, this.receivedMsgs.size());
		// assertTrue(this.receivedMsgs.get(3) instanceof BGPNotificationMessage);
		// final BGPMessage m = this.clientListener.getListMsg().get(3);
		// assertEquals(BGPError.FSM_ERROR, ((BGPNotificationMessage) m).getError());
	}

	@Test
	public void testNotAccChars() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof BGPOpenMessage);
		this.clientSession.handleMessage(new BGPOpenMessage(new ASNumber(30), (short) 1, null, null));
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof BGPNotificationMessage);
		final BGPMessage m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
		assertEquals(BGPError.UNSPECIFIC_OPEN_ERROR, ((BGPNotificationMessage) m).getError());
	}

	@Test
	@Ignore
	// long duration
	public void testNoOpen() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof BGPOpenMessage);
		Thread.sleep(BGPSessionNegotiator.INITIAL_HOLDTIMER * 1000 * 60);
		Thread.sleep(100);
		final BGPMessage m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
		assertEquals(BGPError.HOLD_TIMER_EXPIRED, ((BGPNotificationMessage) m).getError());
	}

	@Test
	public void sendNotification() {
		this.clientSession.channelActive(null);
		this.clientSession.handleMessage(this.classicOpen);
		this.clientSession.handleMessage(new BGPKeepAliveMessage());
		assertEquals(this.clientSession.getState(), BGPSessionNegotiator.State.Finished);
		try {
			this.clientSession.handleMessage(new BGPOpenMessage(new ASNumber(30), (short) 3, null, null));
			fail("Exception should be thrown.");
		} catch (final IllegalStateException e) {
			assertEquals("Unexpected state Finished", e.getMessage());
		}
	}

	@After
	public void tearDown() {

	}
}
