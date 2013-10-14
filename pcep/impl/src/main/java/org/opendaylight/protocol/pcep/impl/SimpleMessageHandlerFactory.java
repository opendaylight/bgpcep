/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleMessageHandlerFactory implements MessageHandlerRegistry {
	public static final MessageHandlerRegistry INSTANCE;

	static {
		final MessageHandlerRegistry reg = new SimpleMessageHandlerFactory();

		// FIXME: fill this in
		// reg.registerMessageHandler(PCEPOpenMessage.class, 1, new PCEPOpenMessageParser());
		// reg.registerMessageHandler(PCEPNotificationMessage.class, 5, new PCEPNotificationMessageParser());
		// reg.registerMessageHandler(PCEPKeepAliveMessage.class, 2, new PCEPKeepAliveMessageParser());
		// reg.registerMessageHandler(PCEPReplyMessage.class, 4, new PCEPReplyMessageParser());
		// reg.registerMessageHandler(PCEPRequestMessage.class, 3, new PCEPRequestMessageParser());
		// reg.registerMessageHandler(PCEPErrorMessage.class, 6, new PCEPErrorMessageParser());
		// reg.registerMessageHandler(PCEPCloseMessage.class, 7, new PCEPCloseMessageParser());
		// reg.registerMessageHandler(PCEPUpdateRequestMessage.class, 11, new PCEPUpdateRequestMessageParser());
		// reg.registerMessageHandler(PCEPReportMessage.class, 10, new PCEPReportMessageParser());
		// reg.registerMessageHandler(PCCreateMessage.class, 12, new PCCreateMessageParser());

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return handlers.registerParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		return handlers.registerSerializer(msgClass, serializer);
	}

	@Override
	public MessageParser getMessageParser(final int messageType) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return handlers.getParser(messageType);
	}

	@Override
	public MessageSerializer getMessageSerializer(final Message message) {
		return handlers.getSerializer(message.getImplementedInterface());
	}
}
