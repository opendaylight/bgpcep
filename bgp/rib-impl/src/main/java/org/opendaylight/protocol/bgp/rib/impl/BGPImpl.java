/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposalChecker;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;

/**
 * Implementation of {@link BGP}.
 */
public class BGPImpl implements BGP, Closeable {
	/**
	 * Wrapper class to give listener a close method.
	 */
	public class BGPListenerRegistration implements ListenerRegistration<BGPSessionListener> {

		private final BGPSessionListener listener;

		private final BGPSession session;

		public BGPListenerRegistration(final BGPSessionListener l, final BGPSession session) {
			this.listener = l;
			this.session = session;
		}

		@Override
		public void close() {
			this.session.close();
		}

		@Override
		public BGPSessionListener getListener() {
			return this.listener;
		}
	}

	private final BGPDispatcher dispatcher;

	private final ProtocolMessageFactory parser;

	private final InetSocketAddress address;

	private final BGPSessionProposal proposal;

	private final BGPSessionProposalChecker checker;

	public BGPImpl(final BGPDispatcher dispatcher, final ProtocolMessageFactory parser, final InetSocketAddress address,
			final BGPSessionProposal proposal, final BGPSessionProposalChecker checker) throws IOException {
		this.dispatcher = dispatcher;
		this.parser = parser;
		this.address = address;
		this.proposal = proposal;
		this.checker = checker;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListenerRegistration<BGPSessionListener> registerUpdateListener(final BGPSessionListener listener) throws IOException {
		final BGPSession session = this.dispatcher.createClient(
				new BGPConnectionImpl(this.address, listener, this.proposal.getProposal(), this.checker), this.parser);
		return new BGPListenerRegistration(listener, session);
	}

	@Override
	public void close() {

	}
}
