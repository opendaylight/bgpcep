/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandler;

public class ProtocolHandlerFactory {

	private final ProtocolMessageEncoder encoder;

	private final ProtocolMessageDecoder decoder;

	public ProtocolHandlerFactory(final ProtocolMessageFactory msgFactory) {
		super();
		this.encoder = new ProtocolMessageEncoder(msgFactory);
		this.decoder = new ProtocolMessageDecoder(msgFactory);
	}

	public ChannelHandler getEncoder() {
		return this.encoder;
	}

	public ChannelHandler getDecoder() {
		return this.decoder;
	}

	public ChannelHandler getSessionInboundHandler(final ProtocolSession session) {
		return new ProtocolSessionInboundHandler(session);
	}
}
