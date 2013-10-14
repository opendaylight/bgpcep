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

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.Open;

/**
 * Parser for {@link OpenMessage}
 */
public class PCEPOpenMessageParser extends AbstractMessageParser {

	private final int TYPE = 1;

	public PCEPOpenMessageParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof OpenMessage))
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance " + message.getClass() + ". Nedded OpenMessage.");
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessage open = ((OpenMessage) message).getOpenMessage();

		if (open.getOpen() == null) {
			throw new IllegalArgumentException("Open Object must be present in Open Message.");
		}

		buffer.writeBytes(serializeObject(open.getOpen()));
	}

	@Override
	public OpenMessage parseMessage(final byte[] buffer) throws PCEPDeserializerException, PCEPDocumentedException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Open message doesn't contain OPEN object.");
		}
		final List<Object> objs = parseObjects(buffer);

		return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder().setOpenMessage(
				validate(objs)).build();
	}

	private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessage validate(
			final List<Object> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		if (objects.isEmpty() || !(objects.get(0) instanceof Open))
			throw new PCEPDeserializerException("Open message doesn't contain OPEN object.");

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessage msg = new OpenMessageBuilder().setOpen(
				(Open) objects.get(0)).build();

		objects.remove(0);

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return msg;
	}

	@Override
	public int getMessageType() {
		return this.TYPE;
	}
}
