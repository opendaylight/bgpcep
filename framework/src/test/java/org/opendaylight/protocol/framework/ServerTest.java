/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Test;

public class ServerTest {
	private static final int MAX_MSGSIZE = 500;
	public static final int PORT = 18080;

	DispatcherImpl clientDispatcher, dispatcher;

	final SimpleSessionListener pce = new SimpleSessionListener();

	ProtocolSession session = null;

	ProtocolServer server = null;

	@Test
	public void testConnectionEstablished() throws Exception {
		this.dispatcher = new DispatcherImpl(new MessageFactory());

		final InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.3", PORT);

		this.server = this.dispatcher.createServer(serverAddress, new ProtocolConnectionFactory() {
			@Override
			public ProtocolConnection createProtocolConnection(final InetSocketAddress address) {

				return new ProtocolConnection() {
					@Override
					public SessionPreferencesChecker getProposalChecker() {
						return new SimpleSessionProposalChecker();
					}

					@Override
					public SessionPreferences getProposal() {
						return new SimpleSessionPreferences();
					}

					@Override
					public InetSocketAddress getPeerAddress() {
						return address;
					}

					@Override
					public SessionListener getListener() {
						return new SimpleSessionListener();
					}
				};
			}
		}, new SimpleSessionFactory(MAX_MSGSIZE));

		this.clientDispatcher = new DispatcherImpl(new MessageFactory());

		this.session = this.clientDispatcher.createClient(new ProtocolConnection() {
			@Override
			public SessionPreferencesChecker getProposalChecker() {
				return new SimpleSessionProposalChecker();
			}

			@Override
			public SessionPreferences getProposal() {
				return new SimpleSessionPreferences();
			}

			@Override
			public InetSocketAddress getPeerAddress() {
				return new InetSocketAddress("127.0.0.3", PORT);
			}

			@Override
			public SessionListener getListener() {
				return ServerTest.this.pce;
			}
		}, new SimpleSessionFactory(MAX_MSGSIZE)).get();

		final int maxAttempts = 1000;
		int attempts = 0;
		synchronized (this.pce) {
			while (!this.pce.up && ++attempts < maxAttempts) {
				this.pce.wait(100);
			}
		}
		assertTrue(this.pce.up);
	}

	@Test
	public void testConnectionFailed() throws IOException, InterruptedException, ExecutionException {
		this.dispatcher = new DispatcherImpl(new MessageFactory());
		this.clientDispatcher = new DispatcherImpl(new MessageFactory());
		final SimpleSessionListener listener = new SimpleSessionListener();

		final Future<ProtocolSession> session = this.clientDispatcher.createClient(new ProtocolConnection() {
			@Override
			public SessionPreferencesChecker getProposalChecker() {
				return new SimpleSessionProposalChecker();
			}

			@Override
			public SessionPreferences getProposal() {
				return new SimpleSessionPreferences();
			}

			@Override
			public InetSocketAddress getPeerAddress() {
				return new InetSocketAddress("127.0.0.5", PORT);
			}

			@Override
			public SessionListener getListener() {
				return listener;
			}
		}, new SimpleSessionFactory(MAX_MSGSIZE));
		try {
			session.get();
			fail("Exception should have occurred.");
		} catch (final ExecutionException e) {
			listener.failed = true;
			assertTrue(e.getCause() instanceof ConnectException);
		}
		final int maxAttempts = 100;
		int attempts = 0;
		synchronized (listener) {
			while (!listener.failed && ++attempts < maxAttempts) {
				listener.wait(100);
			}
		}
		assertTrue(listener.failed);
	}

	@After
	public void tearDown() throws IOException {
		this.dispatcher.onSessionClosed(this.session);
		if (this.server != null)
			this.server.close();
		// this.dispatcher.stop();
		// this.clientDispatcher.stop();
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
