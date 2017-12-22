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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpByteToMessageDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(BmpByteToMessageDecoder.class);
    private final BmpMessageRegistry registry;

    public BmpByteToMessageDecoder(final BmpMessageRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws BmpDeserializationException {
        if (in.isReadable()) {
            LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            out.add(this.registry.parseMessage(in));
        } else {
            LOG.trace("No more content in incoming buffer.");
        }
    }

}
