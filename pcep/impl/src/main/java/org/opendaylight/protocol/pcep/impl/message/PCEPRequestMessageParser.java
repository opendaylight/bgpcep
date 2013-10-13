/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;

/**
 * Parser for {@link PcrepMessage}
 */
//FIXME finish
public class PCEPRequestMessageParser extends AbstractMessageParser {
	
	private final int TYPE = 3;

	public PCEPRequestMessageParser(HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(Message message, ByteBuf buffer) {
		if (!(message instanceof PcrepMessage))
			throw new IllegalArgumentException("Wrong instance of PCEPMessage. Passed instance of " + message.getClass()
					+ ". Needed PcrepMessage.");
	}

	@Override
	public PcrepMessage parseMessage(byte[] buffer) throws PCEPDeserializerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
