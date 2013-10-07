/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;

/**
 * Parser for {@link KeepaliveMessage}
 */
public class PCEPKeepAliveMessageParser extends AbstractMessageParser {
	
	private final int TYPE = 2;

	public PCEPKeepAliveMessageParser(HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(Message message, ByteBuf buffer) {
		if (!(message instanceof KeepaliveMessage))
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded KeepaliveMessage.");

		buffer.writeBytes(new byte[0]);
	}

	@Override
	public KeepaliveMessage parseMessage(byte[] buffer) {
		return new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
