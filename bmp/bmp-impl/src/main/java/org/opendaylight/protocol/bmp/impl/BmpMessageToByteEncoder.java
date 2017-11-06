/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class BmpMessageToByteEncoder extends MessageToByteEncoder<Notification> {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMessageToByteEncoder.class);

    private final BmpMessageRegistry registry;

    public BmpMessageToByteEncoder(final BmpMessageRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Notification message, final ByteBuf out)
        throws Exception {
        LOG.trace("Encoding message: {}", message);
        this.registry.serializeMessage(message, out);
        LOG.debug("Message sent to output: {}", message);
    }

}
