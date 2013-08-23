/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.Dispatcher;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.framework.ReconnectStrategy;

import com.google.common.base.Preconditions;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl implements BGPDispatcher {
	private final Timer timer = new HashedWheelTimer();
	private final ProtocolMessageFactory<BGPMessage> parser;
	private final Dispatcher dispatcher;

	public BGPDispatcherImpl(final Dispatcher dispatcher, final ProtocolMessageFactory<BGPMessage> parser) {
		this.dispatcher = Preconditions.checkNotNull(dispatcher);
		this.parser = Preconditions.checkNotNull(parser);
	}

	@Override
	public Future<? extends BGPSession> createClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
			final BGPSessionListener listener, final ReconnectStrategy strategy) {
		return this.dispatcher.createClient(address, listener, new BGPSessionNegotiatorFactory(timer, preferences), parser, strategy);
	}

	public Dispatcher getDispatcher() {
		return this.dispatcher;
	}
}
