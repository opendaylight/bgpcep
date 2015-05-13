/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.registry;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.BmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpMessageSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBmpMessageRegistry implements BmpMessageRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleBmpMessageRegistry.class);

    private final HandlerRegistry<DataContainer, BmpMessageParser, BmpMessageSerializer> handlers = new HandlerRegistry<>();

    @Override
    public AutoCloseable registerBmpMessageParser(final int messageType, final BmpMessageParser parser) {
        Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(messageType, parser);
    }

    @Override
    public AutoCloseable registerBmpMessageSerializer(final Class<? extends Notification> msgClass, final BmpMessageSerializer serializer) {
        return this.handlers.registerSerializer(msgClass, serializer);
    }

    @Override
    public Notification parseMessage(final int messageType, final ByteBuf buffer) {
        Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        final BmpMessageParser parser = this.handlers.getParser(messageType);
        if (parser == null) {
            LOG.warn("BMP parser for message type {} is not registered.", messageType);
            return null;
        }
        return parser.parseMessage(buffer);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        final BmpMessageSerializer serializer = this.handlers.getSerializer(message.getImplementedInterface());
        if (serializer == null) {
            LOG.warn("BMP serializer for message type {} is not registered.", message.getClass());
            return;
        }
        serializer.serializeMessage(message, buffer);
    }

}
