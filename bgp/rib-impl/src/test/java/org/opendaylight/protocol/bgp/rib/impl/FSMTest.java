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
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPTableType;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.Lists;

public class FSMTest {

	private BGPSessionNegotiator clientSession;

	@Mock
	private Channel speakerListener;

	@Mock
	private ChannelPipeline pipeline;

	private final BGPTableType ipv4tt = new BGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

	private final BGPTableType linkstatett = new BGPTableType(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);

	private final List<Notification> receivedMsgs = Lists.newArrayList();

	private Open classicOpen;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		final List<BgpParameters> tlvs = Lists.newArrayList();

		tlvs.add((BgpParameters) new CMultiprotocolBuilder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(
				this.ipv4tt.getAddressFamily()).setSafi(this.ipv4tt.getSubsequentAddressFamily()).build()));
		tlvs.add((BgpParameters) new CMultiprotocolBuilder().setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(
				this.linkstatett.getAddressFamily()).setSafi(this.linkstatett.getSubsequentAddressFamily()).build()));
		final BGPSessionPreferences prefs = new BGPSessionPreferences(30, (short) 3, null, tlvs);
		this.clientSession = new BGPSessionNegotiator(new HashedWheelTimer(), new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE), this.speakerListener, prefs, new SimpleSessionListener());
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) {
				final Object[] args = invocation.getArguments();
				FSMTest.this.receivedMsgs.add((Notification) args[0]);
				return null;
			}
		}).when(this.speakerListener).writeAndFlush(any(Notification.class));
		doReturn("TestingChannel").when(this.speakerListener).toString();
		doReturn(this.pipeline).when(this.speakerListener).pipeline();
		doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
		doReturn(mock(ChannelFuture.class)).when(this.speakerListener).close();
		this.classicOpen = new OpenBuilder().setMyAsNumber(30).setHoldTimer(3).setVersion(new ProtocolVersion((short) 4)).setBgpParameters(
				tlvs).build();
	}

	@Test
	@Ignore
	// FIXME BUG-100
	public void testAccSessionChar() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.clientSession.handleMessage(this.classicOpen);
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof Keepalive);
		this.clientSession.handleMessage(new KeepaliveBuilder().build());
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
	@Ignore
	// FIXME BUG-100
	public void testNotAccChars() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		this.clientSession.handleMessage(new OpenBuilder().setMyAsNumber(30).setHoldTimer(1).setVersion(new ProtocolVersion((short) 4)).build());
		assertEquals(2, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(1) instanceof Notify);
		final Notification m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
		assertEquals(BGPError.UNSPECIFIC_OPEN_ERROR, BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
	}

	@Test
	@Ignore
	// long duration
	public void testNoOpen() throws InterruptedException {
		this.clientSession.channelActive(null);
		assertEquals(1, this.receivedMsgs.size());
		assertTrue(this.receivedMsgs.get(0) instanceof Open);
		Thread.sleep(BGPSessionNegotiator.INITIAL_HOLDTIMER * 1000 * 60);
		Thread.sleep(100);
		final Notification m = this.receivedMsgs.get(this.receivedMsgs.size() - 1);
		assertEquals(BGPError.HOLD_TIMER_EXPIRED, BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
	}

	@Test
	@Ignore
	// FIXME BUG-100
	public void sendNotification() {
		this.clientSession.channelActive(null);
		this.clientSession.handleMessage(this.classicOpen);
		this.clientSession.handleMessage(new KeepaliveBuilder().build());
		assertEquals(this.clientSession.getState(), BGPSessionNegotiator.State.Finished);
		try {
			this.clientSession.handleMessage(new OpenBuilder().setMyAsNumber(30).setHoldTimer(3).setVersion(new ProtocolVersion((short) 4)).build());
			fail("Exception should be thrown.");
		} catch (final IllegalStateException e) {
			assertEquals("Unexpected state Finished", e.getMessage());
		}
	}

	@After
	public void tearDown() {

	}
}
