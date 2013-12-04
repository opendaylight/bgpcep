/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

/**
 *
 */
@Sharable
public final class PCEPMessageToByteEncoder extends MessageToByteEncoder<Message> {
	private static final Logger LOG = LoggerFactory.getLogger(PCEPMessageToByteEncoder.class);
	private static final int VERSION_SF_LENGTH = 3;
	private final MessageHandlerRegistry registry;

	public PCEPMessageToByteEncoder(final MessageHandlerRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
	}

	@Override
	protected void encode(final ChannelHandlerContext ctx, final Message msg, final ByteBuf out) throws Exception {
		Preconditions.checkNotNull(msg);
		LOG.debug("Sent to encode : {}", msg);

		final ByteBuf body = Unpooled.buffer();
		final MessageSerializer serializer = this.registry.getMessageSerializer(msg);
		serializer.serializeMessage(msg, body);

		final int msgLength = body.readableBytes() + PCEPMessageConstants.COMMON_HEADER_LENGTH;
		final byte[] header = new byte[] {
				UnsignedBytes.checkedCast(PCEPMessageConstants.PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH)),
				UnsignedBytes.checkedCast(serializer.getMessageType()),
				UnsignedBytes.checkedCast(msgLength / 256),
				UnsignedBytes.checkedCast(msgLength % 256)
		};
		Preconditions.checkState(header.length == PCEPMessageConstants.COMMON_HEADER_LENGTH);
		out.writeBytes(header);
		out.writeBytes(body);
	}
}
