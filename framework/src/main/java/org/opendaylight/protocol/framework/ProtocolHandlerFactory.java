/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandler;

public class ProtocolHandlerFactory<T extends ProtocolMessage> {
	private final ProtocolMessageDecoder<T> decoder;
	private final ProtocolMessageEncoder<T> encoder;

	public ProtocolHandlerFactory(final ProtocolMessageFactory<T> msgFactory) {
		this.encoder = new ProtocolMessageEncoder<T>(msgFactory);
		this.decoder = new ProtocolMessageDecoder<T>(msgFactory);
	}

	public ChannelHandler getEncoder() {
		return this.encoder;
	}

	public ChannelHandler getDecoder() {
		return this.decoder;
	}
}
