/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
final class ProtocolMessageEncoder extends MessageToByteEncoder<ProtocolMessage> {

	private final ProtocolMessageFactory factory;

	public ProtocolMessageEncoder(final ProtocolMessageFactory factory) {
		this.factory = factory;
	}

	@Override
	protected void encode(final ChannelHandlerContext ctx, final ProtocolMessage msg, final ByteBuf out) throws Exception {
		out.writeBytes(this.factory.put(msg));
	}
}
