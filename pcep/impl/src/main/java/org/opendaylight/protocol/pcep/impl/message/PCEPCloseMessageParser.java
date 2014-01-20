/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CClose;

/**
 * Parser for {@link CloseMessage}
 */
public class PCEPCloseMessageParser extends AbstractMessageParser {

	public static final int TYPE = 7;

	public PCEPCloseMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof CloseMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded CloseMessage.");
		}
		final CCloseMessage close = ((CloseMessage) message).getCCloseMessage();

		if (close.getCClose() == null) {
			throw new IllegalArgumentException("Close Object must be present in Close Message.");
		}
		buffer.writeBytes(serializeObject(close.getCClose()));
	}

	@Override
	protected Close validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		if (objects.isEmpty() || !(objects.get(0) instanceof CClose)) {
			throw new PCEPDeserializerException("Close message doesn't contain CLOSE object.");
		}
		final Object o = objects.get(0);
		final CCloseMessage msg = new CCloseMessageBuilder().setCClose((CClose) o).build();
		objects.remove(0);
		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		return new CloseBuilder().setCCloseMessage(msg).build();
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
