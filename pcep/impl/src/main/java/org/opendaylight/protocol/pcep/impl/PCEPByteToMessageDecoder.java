/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPMessageConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PCEP message parser which also does validation.
 */
public final class PCEPByteToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPByteToMessageDecoder.class);

    private final MessageRegistry registry;

    public PCEPByteToMessageDecoder(final MessageRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        if (!in.isReadable()) {
            LOG.debug("No more content in incoming buffer.");
            return;
        }

        in.markReaderIndex();
        LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));

        final List<Message> errors = new ArrayList<>();

        try {
            out.add(parse(in, errors));
        } catch (final PCEPDeserializerException e) {
            LOG.warn("Failed to decode protocol message", e);
        }
        in.discardReadBytes();

        if (!errors.isEmpty()) {
            // We have a bunch of messages, send them out
            for (final Object e : errors) {
                ctx.channel().writeAndFlush(e).addListener((ChannelFutureListener) f -> {
                    if (!f.isSuccess()) {
                        LOG.warn("Failed to send message {} to socket {}", e, ctx.channel(), f.cause());
                    } else {
                        LOG.trace("Message {} sent to socket {}", e, ctx.channel());
                    }
                });
            }
        }
    }

    private Message parse(final ByteBuf buffer, final List<Message> errors) throws PCEPDeserializerException {
        buffer.skipBytes(1);
        final int type = buffer.readUnsignedByte();
        final int msgLength = buffer.readUnsignedShort();
        final int actualLength = buffer.readableBytes();
        final ByteBuf msgBody = buffer.slice();
        if (actualLength != msgLength - PCEPMessageConstants.COMMON_HEADER_LENGTH) {
            throw new PCEPDeserializerException("Body size " + actualLength + " does not match header size "
                    + (msgLength - PCEPMessageConstants.COMMON_HEADER_LENGTH));
        }
        buffer.skipBytes(actualLength);
        return this.registry.parseMessage(type, msgBody, errors);
    }
}
