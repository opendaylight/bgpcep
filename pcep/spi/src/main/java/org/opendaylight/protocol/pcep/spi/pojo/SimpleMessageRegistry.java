/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleMessageRegistry implements MessageRegistry {

	private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
		return this.handlers.registerParser(messageType, parser);
	}

	public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		return this.handlers.registerSerializer(msgClass, serializer);
	}

	@Override
	public Message parseMessage(int messageType, byte[] buffer, List<Message> errors) throws PCEPDeserializerException {
		Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
		final MessageParser parser = this.handlers.getParser(messageType);
		if (parser == null) {
			return null;
		}
		return parser.parseMessage(buffer, errors);
	}

	@Override
	public void serializeMessage(Message message, ByteBuf buffer) {
		final MessageSerializer serializer = this.handlers.getSerializer(message.getImplementedInterface());
		if (serializer == null) {
			return;
		}
		serializer.serializeMessage(message, buffer);
	}
}
