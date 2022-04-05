/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractMessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

final class SimpleMessageRegistry extends AbstractMessageRegistry {

    private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

    @Override
    protected Notification<?> parseBody(final int type, final ByteBuf body, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        final MessageParser parser = handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseMessageBody(body, messageLength, constraint);
    }

    @Override
    protected void serializeMessageImpl(final Notification<?> message, final ByteBuf buffer) {
        final MessageSerializer serializer = handlers.getSerializer(message.implementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeMessage(message, buffer);
    }

    Registration registerMessageParser(final int messageType, final MessageParser parser) {
        return handlers.registerParser(messageType, parser);
    }

    <T extends Notification<T> & DataObject> Registration registerMessageSerializer(final Class<T> messageClass,
            final MessageSerializer serializer) {
        return handlers.registerSerializer(messageClass, serializer);
    }
}
