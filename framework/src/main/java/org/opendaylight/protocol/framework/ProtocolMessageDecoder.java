/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MessageList;
import io.netty.handler.codec.ByteToMessageDecoder;

final class ProtocolMessageDecoder extends ByteToMessageDecoder {

	private final ProtocolMessageFactory factory;

	public ProtocolMessageDecoder(final ProtocolMessageFactory factory) {
		this.factory = factory;
	}

	@Override
	public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final MessageList<Object> out) throws Exception {
		ProtocolMessage msg = null;
		try {
			msg = this.factory.parse(in.array());
		} catch (DeserializerException | DocumentedException e) {
			this.exceptionCaught(ctx, e);
		}
		out.add(msg);
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
		// TODO:
		ctx.close();
	}
}
