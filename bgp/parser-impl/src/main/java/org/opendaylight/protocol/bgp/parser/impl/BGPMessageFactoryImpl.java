/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPKeepAliveMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPMessageFactoryImpl extends AbstractMessageRegistry {

	public static final BGPMessageFactory INSTANCE;

	static {
		final BGPMessageFactoryImpl reg = new BGPMessageFactoryImpl();

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

	private final HandlerRegistry<Notification, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

	private BGPMessageFactoryImpl() {

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
		final MessageSerializer serializer = handlers.getSerializer(message);
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
