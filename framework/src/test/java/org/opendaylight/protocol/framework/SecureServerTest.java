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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.util.SSLUtil;

public class SecureServerTest {
	static final Random rnd = new Random();

	private static final int MAX_MSGSIZE = 500;
	final SimpleSessionListener pce = new SimpleSessionListener();
	ProtocolSession session;
	ProtocolServer server;
	DispatcherImpl dispatcher;
	SSLContext context;
	int port;

	@Before
	public void setUp() throws Exception {
		final InputStream keyStore = SecureServerTest.class.getResourceAsStream("/keystore.jks");
		final InputStream trustStore = SecureServerTest.class.getResourceAsStream("/keystore.jks");
		this.context = SSLUtil.initializeSecureContext("keystore", keyStore, trustStore, "SunX509");
		keyStore.close();
		trustStore.close();

		this.dispatcher = new DispatcherImpl(Executors.defaultThreadFactory());
		this.port = rnd.nextInt(10000) + 20000;
	}

	@After
	public void tearDown() throws IOException {
		this.dispatcher.onSessionClosed(this.session);
		this.server.close();
		this.dispatcher.stop();
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testServerConnection() throws Exception {

		this.server = this.dispatcher.createServer(new InetSocketAddress("127.0.0.3", this.port), new ProtocolConnectionFactory() {
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
		}, new SimpleSessionFactory(MAX_MSGSIZE), SimpleInputStream.FACTORY, this.context);

		try {
			this.server = this.dispatcher.createServer(new InetSocketAddress("127.0.0.3", this.port), null, null, null, null);
			fail("Exception should have occured.");
		} catch (final IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Server with this address:") && e.getMessage().endsWith("was already created."));
		}

		this.session = this.dispatcher.createClient(new ProtocolConnection() {
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
				return new InetSocketAddress("127.0.0.3", SecureServerTest.this.port);
			}

			@Override
			public SessionListener getListener() {
				return SecureServerTest.this.pce;
			}
		}, new SimpleSessionFactory(MAX_MSGSIZE), SimpleInputStream.FACTORY, this.context);

		try {
			this.session = this.dispatcher.createClient(new ProtocolConnection() {
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
					return new InetSocketAddress("127.0.0.3", SecureServerTest.this.port);
				}

				@Override
				public SessionListener getListener() {
					return SecureServerTest.this.pce;
				}
			}, null, null, null);
			fail("Exception should have occured.");
		} catch (final IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Attempt to create duplicate client session to the same address:"));
		}

		synchronized (this.pce) {
			while (!this.pce.up)
				this.pce.wait();
		}
	}

	@Test
	public void testIO() throws IOException, InterruptedException, KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException {
		this.server = this.dispatcher.createServer(new InetSocketAddress("127.0.0.3", this.port), new ProtocolConnectionFactory() {
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
		}, new ProtocolSessionFactory() {
			@Override
			public ProtocolSession getProtocolSession(final SessionParent parent, final Timer timer, final ProtocolConnection connection,
					final int sessionId) {
				return new Session(parent, MAX_MSGSIZE);
			}
		}, SimpleInputStream.FACTORY, this.context);

		this.session = this.dispatcher.createClient(new ProtocolConnection() {
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
				return new InetSocketAddress("127.0.0.3", SecureServerTest.this.port);
			}

			@Override
			public SessionListener getListener() {
				return new SimpleSessionListener();
			}
		}, new ProtocolSessionFactory() {
			@Override
			public ProtocolSession getProtocolSession(final SessionParent parent, final Timer timer, final ProtocolConnection connection,
					final int sessionId) {
				return new Session(parent, MAX_MSGSIZE);
			}
		}, SimpleInputStream.FACTORY, this.context);
	}

}
