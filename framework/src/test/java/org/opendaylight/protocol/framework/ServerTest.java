/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

public class ServerTest {
	public static final int PORT = 18080;

	AbstractDispatcher clientDispatcher, dispatcher;

	final SimpleSessionListener pce = new SimpleSessionListener();

	SimpleSession session = null;

	ChannelFuture server = null;

	public final InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.5", PORT);

	@Test
	public void testConnectionEstablished() throws Exception {
		this.dispatcher = new AbstractDispatcher() { };

		final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

		this.server = this.dispatcher.createServer(this.serverAddress,
				new SessionListenerFactory<SimpleSessionListener>() {
			@Override
			public SimpleSessionListener getSessionListener() {
				return new SimpleSessionListener();
			}
		}, new SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener>() {

			@Override
			public SessionNegotiator<SimpleSession> getSessionNegotiator(final SessionListenerFactory<SimpleSessionListener> factory,
					final Channel channel, final Promise<SimpleSession> promise) {
				p.setSuccess(true);
				return new SimpleSessionNegotiator(promise, channel);
			}
		}, new MessageFactory());

		server.get();

		this.clientDispatcher = new AbstractDispatcher() { };

		this.session = this.clientDispatcher.createClient(serverAddress,
				new SimpleSessionListener(), new SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener>() {
			@Override
			public SessionNegotiator<SimpleSession> getSessionNegotiator(final SessionListenerFactory<SimpleSessionListener> factory,
					final Channel channel, final Promise<SimpleSession> promise) {
				return new SimpleSessionNegotiator(promise, channel);
			}
		}, new MessageFactory(), new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000)).get();

		assertEquals(true, p.get(3, TimeUnit.SECONDS));
	}

	public void testConnectionFailed() throws IOException, InterruptedException {
		this.dispatcher = new AbstractDispatcher() { };
		this.clientDispatcher = new AbstractDispatcher() { };
		final SimpleSessionListener listener = new SimpleSessionListener();

		try {
			this.clientDispatcher.createClient(serverAddress, listener,
					new SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener>() {
				@Override
				public SessionNegotiator<SimpleSession> getSessionNegotiator(final SessionListenerFactory<SimpleSessionListener> factory,
						final Channel channel, final Promise<SimpleSession> promise) {
					// TODO Auto-generated method stub
					return null;
				}
			}, new MessageFactory(), new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000)).get();

			fail("Connection succeeded unexpectedly");
		} catch (ExecutionException e) {
			assertTrue(listener.failed);
			assertTrue(e.getCause() instanceof ConnectException);
		}
	}

	@After
	public void tearDown() throws IOException {
		server.channel().close();
		this.dispatcher.close();
		this.clientDispatcher.close();
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
