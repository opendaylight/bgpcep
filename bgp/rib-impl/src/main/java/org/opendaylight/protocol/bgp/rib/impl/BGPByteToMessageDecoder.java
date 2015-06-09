/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
final class BGPByteToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(BGPByteToMessageDecoder.class);
    private final MessageRegistry registry;

    public BGPByteToMessageDecoder(final MessageRegistry registry) {
        this.registry = Preconditions.checkNotNull(registry);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws BGPDocumentedException,
            BGPParsingException {
        if (in.isReadable()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
            }
            out.add(this.registry.parseMessage(in));
        } else {
            LOG.trace("No more content in incoming buffer.");
        }
    }
}
