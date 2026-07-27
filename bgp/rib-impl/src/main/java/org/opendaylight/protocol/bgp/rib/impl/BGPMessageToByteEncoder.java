/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.yangtools.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
final class BGPMessageToByteEncoder extends MessageToByteEncoder<Notification<?>> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPMessageToByteEncoder.class);
    private final MessageRegistry registry;

    BGPMessageToByteEncoder(final MessageRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Notification<?> msg, final ByteBuf out) {
        serialize(msg, out);
    }

    /**
     * Serialize a message into a new buffer. The caller hands that buffer to the channel, so netty adds the
     * real size of the message to the pending bytes of the channel instead of an estimate.
     *
     * @param alloc allocator to take the buffer from
     * @param msg message to serialize
     * @return a buffer holding the serialized message, owned by the caller
     */
    ByteBuf encodeToBuffer(@NonNull final ByteBufAllocator alloc, @NonNull final Notification<?> msg) {
        final var out = alloc.ioBuffer();
        var serialized = false;
        try {
            serialize(msg, out);
            serialized = true;
        } finally {
            // Release buffer if serialize did not succeed.
            if (!serialized) {
                out.release();
            }
        }
        return out;
    }

    private void serialize(final Notification<?> msg, final ByteBuf out) {
        LOG.trace("Encoding message: {}", msg);
        registry.serializeMessage(msg, out);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Encoded message: {}", ByteBufUtil.hexDump(out));
        }
        LOG.debug("Message sent to output: {}", msg);
    }
}
