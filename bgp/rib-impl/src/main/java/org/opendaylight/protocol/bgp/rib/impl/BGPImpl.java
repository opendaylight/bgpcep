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
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * Implementation of {@link BGP}.
 */
public class BGPImpl implements BGP, Closeable {
	private final BGPDispatcher dispatcher;

	private final InetSocketAddress address;

	private final BGPSessionProposal proposal;

	public BGPImpl(final BGPDispatcher dispatcher, final InetSocketAddress address, final BGPSessionProposal proposal) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher);
		this.address = Preconditions.checkNotNull(address);
		this.proposal = Preconditions.checkNotNull(proposal);
	}

	@Override
	public ListenerRegistration<BGPSessionListener> registerUpdateListener(final BGPSessionListener listener, final ReconnectStrategyFactory tcpStrategyFactory, final ReconnectStrategy sessionStrategy) {
		final Future<Void> s = this.dispatcher.createReconnectingClient(address, this.proposal.getProposal(), listener, tcpStrategyFactory, sessionStrategy);
		return new ListenerRegistration<BGPSessionListener>() {
			@Override
			public BGPSessionListener getListener() {
				return listener;
			}

			@Override
			public void close() {
				s.cancel(true);
			}
		};
	}

	@Override
	public void close() {

	}
}
