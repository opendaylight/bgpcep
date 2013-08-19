/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.io.IOException;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.framework.Dispatcher;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl implements BGPDispatcher, Closeable {

	private final Dispatcher dispatcher;

	public BGPDispatcherImpl(final Dispatcher dispatcher) throws IOException {
		this.dispatcher = dispatcher;
	}

	@Override
	public Future<? extends BGPSession> createClient(final BGPConnection connection, final ProtocolMessageFactory parser) throws IOException {
		return this.dispatcher.createClient(connection, new BGPSessionFactory(parser));
	}

	public Dispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	public void close() {
		// This is only necessary for configuration interaction
	}
}
