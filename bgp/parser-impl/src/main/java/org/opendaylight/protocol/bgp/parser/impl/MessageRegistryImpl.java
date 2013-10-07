/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.bgp.parser.impl.message.BGPKeepAliveMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

public final class MessageRegistryImpl implements MessageRegistry {
	public static final MessageRegistry INSTANCE;

	static {
		final MessageRegistry reg = new MessageRegistryImpl();

		reg.registerMessageParser(1, BGPOpenMessageParser.PARSER);
		reg.registerMessageSerializer(Open.class, BGPOpenMessageParser.SERIALIZER);
		reg.registerMessageParser(2, BGPUpdateMessageParser.PARSER);
		// Serialization of Update message is not supported
		reg.registerMessageParser(3, BGPNotificationMessageParser.PARSER);
		reg.registerMessageSerializer(Notify.class, BGPNotificationMessageParser.SERIALIZER);
		reg.registerMessageParser(4, BGPKeepAliveMessageParser.PARSER);
		reg.registerMessageSerializer(Keepalive.class, BGPKeepAliveMessageParser.SERIALIZER);

		INSTANCE = reg;
	}

	private final Map<Integer, MessageParser> parsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Notification>, MessageSerializer> serializers = new ConcurrentHashMap<>();

	private synchronized void unregisterMessageParser(final int messageType) {
		parsers.remove(messageType);
	}

	@Override
	public synchronized AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		Preconditions.checkArgument(!parsers.containsKey(messageType), "Message type %s already registered", messageType);
		parsers.put(messageType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageParser(messageType);
			}
		};
	}

	@Override
	public MessageParser getMessageParser(final int messageType) {
		return parsers.get(messageType);
	}

	private synchronized void unregisterMessageSerializer(final Class<? extends Notification> msgClass) {
		serializers.remove(msgClass);
	}

	@Override
	public synchronized AutoCloseable registerMessageSerializer(final Class<? extends Notification> msgClass, final MessageSerializer serializer) {
		Preconditions.checkArgument(!serializers.containsKey(msgClass), "Message class %s already registered", msgClass);
		serializers.put(msgClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageSerializer(msgClass);
			}
		};
	}

	@Override
	public MessageSerializer getMessageSerializer(final Notification message) {
		final Class<? extends Notification> c = message.getClass();

		for (Map.Entry<Class<? extends Notification>, MessageSerializer> e : serializers.entrySet()) {
			if (e.getKey().isAssignableFrom(c)) {
				return e.getValue();
			}
		}

		return null;
	}
}
