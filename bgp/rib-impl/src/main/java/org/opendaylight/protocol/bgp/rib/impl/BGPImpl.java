/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.framework.ReconnectStrategy;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Implementation of {@link BGP}.
 */
public class BGPImpl implements BGP, Closeable {
	/**
	 * Wrapper class to give listener a close method.
	 */
	public static class BGPListenerRegistration implements ListenerRegistration<BGPSessionListener> {

		private final BGPSessionListener listener;

		private BGPSession session;
		private Object bgpSessionLock = new Object();

		public BGPListenerRegistration(final BGPSessionListener l, final Future<? extends BGPSession> sessionFuture) {
			this.listener = l;
			sessionFuture.addListener(new GenericFutureListener<Future<BGPSession>>() {

				@Override
				public void operationComplete(Future<BGPSession> future) throws Exception {
					if (future.isSuccess() == false) {
						throw new IOException("Failed to connect to peer with listener " + listener, future.cause());
					}
					synchronized (bgpSessionLock) {
						session = future.get();
					}
				}
			});
		}

		@Override
		public void close() {
			synchronized (bgpSessionLock) {
				if (session != null)
					this.session.close();
			}
		}

		@Override
		public BGPSessionListener getListener() {
			return this.listener;
		}
	}

	private final BGPDispatcher dispatcher;

	private final InetSocketAddress address;

	private final BGPSessionProposal proposal;

	public BGPImpl(final BGPDispatcher dispatcher, final InetSocketAddress address, final BGPSessionProposal proposal) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher);
		this.address = Preconditions.checkNotNull(address);
		this.proposal = Preconditions.checkNotNull(proposal);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BGPListenerRegistration registerUpdateListener(final BGPSessionListener listener, final ReconnectStrategy strategy) throws IOException {
		final Future<? extends BGPSession> sessionFuture;
		sessionFuture = this.dispatcher.createClient(this.address, this.proposal.getProposal(), listener, strategy);
		return new BGPListenerRegistration(listener, sessionFuture);
	}

	@Override
	public void close() {

	}
}
