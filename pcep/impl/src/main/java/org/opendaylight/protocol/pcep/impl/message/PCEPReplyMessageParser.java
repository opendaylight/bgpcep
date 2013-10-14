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
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;

/**
 * Parser for {@link PcrepMessage}
 */
// FIXME: finish
public class PCEPReplyMessageParser extends AbstractMessageParser {

	private final int TYPE = 4;

	public PCEPReplyMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof PcrepMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded PcrepMessage.");
		}

	}

	@Override
	public PcrepMessage parseMessage(final byte[] buffer) throws PCEPDeserializerException {
		return null;
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
