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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPRecoveredUpdateException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;
import org.opendaylight.protocol.bgp.parser.spi.pojo.PeerSpecificParserConstraintImpl;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPByteToMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(BGPByteToMessageDecoder.class);
    private final MessageRegistry registry;
    private final PeerSpecificParserConstraintProvider constraints;

    BGPByteToMessageDecoder(final MessageRegistry registry) {
        this.constraints = new PeerSpecificParserConstraintImpl();
        this.registry = requireNonNull(registry);
    }

    <T extends PeerConstraint> boolean addDecoderConstraint(final Class<T> classType, final T peerConstraint) {
        return this.constraints.addPeerConstraint(classType, peerConstraint);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
            throws BGPDocumentedException, BGPParsingException {
        if (!in.isReadable()) {
            LOG.trace("No more content in incoming buffer.");
            return;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received to decode: {}", ByteBufUtil.hexDump(in));
        }

        Notification message;
        try {
            message = registry.parseMessage(in, this.constraints);
        } catch (BGPRecoveredUpdateException e) {
            LOG.debug("UPDATE message recovered", e);
            // FIXME: BGPCEP-359: propagate errors so they can be sent out
            message = e.getUpdate();
        }

        out.add(message);
    }
}
