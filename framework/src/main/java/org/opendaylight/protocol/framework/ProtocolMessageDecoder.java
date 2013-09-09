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
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProtocolMessageDecoder<T extends ProtocolMessage> extends ByteToMessageDecoder {

	private final static Logger logger = LoggerFactory.getLogger(ProtocolMessageDecoder.class);

	private final ProtocolMessageFactory<T> factory;

	public ProtocolMessageDecoder(final ProtocolMessageFactory<T> factory) {
		this.factory = factory;
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
		if (in.readableBytes() == 0) {
			logger.debug("No more content in incoming buffer.");
			return;
		}
		in.markReaderIndex();
		try {
			final byte[] bytes = new byte[in.readableBytes()];
			in.readBytes(bytes);
			logger.debug("Received to decode: {}", Arrays.toString(bytes));
			out.addAll(this.factory.parse(bytes));
		} catch (DeserializerException | DocumentedException e) {
			logger.debug("Failed to decode protocol message", e);
			this.exceptionCaught(ctx, e);
		}
		in.discardReadBytes();
	}
}
