/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleMessageRegistry implements MessageRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessageRegistry.class);

    private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

    public Registration registerMessageParser(final int messageType, final MessageParser parser) {
        checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(messageType, parser);
    }

    public Registration registerMessageSerializer(final Class<? extends Message> msgClass,
            final MessageSerializer serializer) {
        return this.handlers.registerSerializer(msgClass, serializer);
    }

    @Override
    public Message parseMessage(final int messageType, final ByteBuf buffer, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        final MessageParser parser = this.handlers.getParser(messageType);
        if (parser == null) {
            LOG.warn("PCEP parser for message type {} is not registered.", messageType);
            return null;
        }
        return parser.parseMessage(buffer, errors);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf buffer) {
        final MessageSerializer serializer = this.handlers.getSerializer(message.implementedInterface());
        if (serializer == null) {
            LOG.warn("PCEP serializer for message type {} is not registered.", message.implementedInterface());
            return;
        }
        serializer.serializeMessage(message, buffer);
    }
}
