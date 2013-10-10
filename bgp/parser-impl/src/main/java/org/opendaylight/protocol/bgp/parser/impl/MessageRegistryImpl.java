/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

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

import com.google.common.base.Preconditions;

public final class MessageRegistryImpl extends HandlerRegistry<Notification, MessageParser, MessageSerializer>
implements MessageRegistry {
	public static final MessageRegistry INSTANCE;

	static {
		final MessageRegistryImpl reg = new MessageRegistryImpl();

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

	private synchronized AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return super.registerParser(messageType, parser);
	}

	@Override
	public MessageParser getMessageParser(final int messageType) {
		return super.getParser(messageType);
	}

	private AutoCloseable registerMessageSerializer(final Class<? extends Notification> msgClass, final MessageSerializer serializer) {
		return super.registerSerializer(msgClass, serializer);
	}

	@Override
	public MessageSerializer getMessageSerializer(final Notification message) {
		return super.getSerializer(message);
	}
}
