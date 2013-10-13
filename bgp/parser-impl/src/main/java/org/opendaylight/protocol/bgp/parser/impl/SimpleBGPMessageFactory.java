/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class SimpleBGPMessageFactory extends AbstractMessageRegistry {
	private static final class Holder {
		private static final SimpleBGPMessageFactory INSTANCE;

		static {
			final SimpleBGPMessageFactory f = new SimpleBGPMessageFactory();
			try {
				new ActivatorImpl().start(SingletonProviderContext.getInstance());
			} catch (Exception e) {
				throw new ExceptionInInitializerError(e);
			}
			INSTANCE = f;
		}
	}

	private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

	private SimpleBGPMessageFactory() {

	}

	public static SimpleBGPMessageFactory getInstance() {
		return Holder.INSTANCE;
	}

	@Override
	protected Notification parseBody(final int type, final byte[] body, final int messageLength) throws BGPDocumentedException {
		final MessageParser parser = handlers.getParser(type);
		if (parser == null) {
			return null;
		}

		return parser.parseMessageBody(body, messageLength);
	}

	@Override
	protected byte[] serializeMessageImpl(final Notification message) {
		final MessageSerializer serializer = handlers.getSerializer(message.getImplementedInterface());
		if (serializer == null) {
			return null;
		}

		return serializer.serializeMessage(message);
	}

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		return handlers.registerParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Notification> messageClass, final MessageSerializer serializer) {
		return handlers.registerSerializer(messageClass, serializer);
	}
}
