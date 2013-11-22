/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

/**
 * A PCEP message parser which also does validation.
 */
public final class PCEPByteToMessageDecoder extends ByteToMessageDecoder {
	private final static Logger LOG = LoggerFactory.getLogger(PCEPByteToMessageDecoder.class);

	private final static int TYPE_SIZE = 1; // bytes

	private final static int LENGTH_SIZE = 2; // bytes

	private final MessageHandlerRegistry registry;

	public PCEPByteToMessageDecoder(final MessageHandlerRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
		if (in.readableBytes() == 0) {
			LOG.debug("No more content in incoming buffer.");
			return;
		}

		in.markReaderIndex();
		LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
		final byte[] bytes = new byte[in.readableBytes()];
		in.readBytes(bytes);

		final List<Message> errors = new ArrayList<>();

		try {
			out.add(parse(bytes, errors));
		} catch (PCEPDeserializerException e) {
			LOG.debug("Failed to decode protocol message", e);
			this.exceptionCaught(ctx, e);
		}
		in.discardReadBytes();

		if (!errors.isEmpty()) {
			// We have a bunch of messages, send them out
			for (final Object e : errors) {
				ctx.channel().write(e);
			}
			ctx.channel().flush();
		}
	}

	private Message parse(final byte[] bytes, final List<Message> errors) throws PCEPDeserializerException {
		final int type = UnsignedBytes.toInt(bytes[1]);
		final int msgLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, TYPE_SIZE + 1, LENGTH_SIZE));

		final byte[] msgBody = ByteArray.cutBytes(bytes, TYPE_SIZE + 1 + LENGTH_SIZE);
		if (msgBody.length != msgLength - PCEPMessageConstants.COMMON_HEADER_LENGTH) {
			throw new PCEPDeserializerException("Body size " + msgBody.length + " does not match header size "
					+ (msgLength - PCEPMessageConstants.COMMON_HEADER_LENGTH));
		}

		final Message msg = this.registry.getMessageParser(type).parseMessage(msgBody, errors);
		LOG.debug("Message was parsed. {}", msg);
		return msg;
	}
}
