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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public final class PCEPMessageToByteEncoder extends MessageToByteEncoder<Message> {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPMessageToByteEncoder.class);

    private final MessageRegistry registry;

    public PCEPMessageToByteEncoder(final MessageRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Message msg, final ByteBuf out) {
        requireNonNull(msg);
        this.registry.serializeMessage(msg, out);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Encoded : {}", ByteBufUtil.hexDump(out));
        }
    }
}
