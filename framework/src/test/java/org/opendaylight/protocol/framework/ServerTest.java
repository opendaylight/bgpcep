/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

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
		this.dispatcher = new DispatcherImpl(Executors.defaultThreadFactory());

		this.server = this.dispatcher.createServer(new InetSocketAddress("127.0.0.3", PORT), new ProtocolConnectionFactory() {
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
		}, new SimpleSessionFactory(MAX_MSGSIZE), SimpleInputStream.FACTORY);

		try {
			this.server = this.dispatcher.createServer(new InetSocketAddress("127.0.0.3", PORT), null, null, null);
			fail("Exception should have occured.");
		} catch (final IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Server with this address:") && e.getMessage().endsWith("was already created."));
		}
		
		this.clientDispatcher = new DispatcherImpl(Executors.defaultThreadFactory());

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
		}, new SimpleSessionFactory(MAX_MSGSIZE), SimpleInputStream.FACTORY);

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
	public void testConnectionFailed() throws IOException, InterruptedException {
		this.dispatcher = new DispatcherImpl(Executors.defaultThreadFactory());
		this.clientDispatcher = new DispatcherImpl(Executors.defaultThreadFactory());

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
		}, new SimpleSessionFactory(MAX_MSGSIZE), SimpleInputStream.FACTORY);
		final int maxAttempts = 1000;
		int attempts = 0;
		synchronized (this.pce) {
			while (!this.pce.failed && ++attempts < maxAttempts) {
				this.pce.wait(100);
			}
		}
		assertTrue(this.pce.failed);
	}

	@After
	public void tearDown() throws IOException {
		this.dispatcher.onSessionClosed(this.session);
		if (this.server != null)
			this.server.close();
		this.dispatcher.stop();
		this.clientDispatcher.stop();
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
