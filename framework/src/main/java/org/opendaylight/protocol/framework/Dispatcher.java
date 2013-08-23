/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * Dispatcher class for creating protocol servers and clients.
 */
public interface Dispatcher {
	/**
	 * Creates server. Each server needs factories to pass their instances to client sessions.
	 * 
	 * @param address address to which the server should be bound
	 * @param listenerFactory factory for creating protocol listeners, passed to the negotiator
	 * @param negotiatorFactory protocol session negotiator factory
	 * @param messageFactory message parser
	 * 
	 * @return ChannelFuture representing the binding process
	 */
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> ChannelFuture createServer(
			InetSocketAddress address,  final SessionListenerFactory<L> listenerFactory,
			SessionNegotiatorFactory<M, S, L> negotiatorFactory, ProtocolMessageFactory<M> messageFactory);

	/**
	 * Creates a client.
	 * 
	 * @param address remote address
	 * @param listener session listener
	 * @param negotiatorFactory session negotiator factory
	 * @param messageFactory message parser
	 * @param connectStrategy Reconnection strategy to be used when initial connection fails
	 * 
	 * @return Future representing the connection process. Its result represents
	 *         the combined success of TCP connection as well as session negotiation.
	 */
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> Future<S> createClient(
			InetSocketAddress address, final L listener, SessionNegotiatorFactory<M, S, L> negotiatorFactory,
			ProtocolMessageFactory<M> messageFactory, ReconnectStrategy connectStrategy);

	/**
	 * Creates a client.
	 * 
	 * @param address remote address
	 * @param listener session listener
	 * @param negotiatorFactory session negotiator factory
	 * @param messageFactory message parser
	 * @param connectStrategyFactory Factory for creating reconnection strategy to be used when initial connection fails
	 * @param reestablishStrategy Reconnection strategy to be used when the already-established session fails
	 * 
	 * @return Future representing the reconnection task. It will report
	 *         completion based on reestablishStrategy, e.g. success if
	 *         it indicates no further attempts should be made and failure
	 *         if it reports an error
	 */
	public <M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> Future<Void> createReconnectingClient(
			final InetSocketAddress address, final L listener, final SessionNegotiatorFactory<M, S, L> negotiatorFactory,
			final ProtocolMessageFactory<M> messageFactory,
			final ReconnectStrategyFactory connectStrategyFactory, final ReconnectStrategy reestablishStrategy);
}
