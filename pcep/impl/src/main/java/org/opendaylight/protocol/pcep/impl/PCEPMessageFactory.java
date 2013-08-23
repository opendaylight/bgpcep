/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.impl.message.PCEPRawMessage;

import com.google.common.base.Preconditions;

/**
 * A PCEP message parser which also does validation.
 */
public final class PCEPMessageFactory implements ProtocolMessageFactory<PCEPMessage> {
	private static final RawPCEPMessageFactory rawFactory = new RawPCEPMessageFactory();

	@Override
	public List<PCEPMessage> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		final List<PCEPMessage> parsed = rawFactory.parse(bytes);
		final List<PCEPMessage> validated = new ArrayList<>(parsed.size());

		for (PCEPMessage msg : parsed) {
			Preconditions.checkState(msg instanceof PCEPRawMessage);
			final PCEPRawMessage raw = (PCEPRawMessage) msg;

			try {
				validated.addAll(PCEPMessageValidator.getValidator(raw.getMsgType()).validate(raw.getAllObjects()));
			} catch (final PCEPDeserializerException e) {
				// FIXME: at validation time we may want to terminate with:
				//logger.error("Malformed message, terminating. ", e);
				// this.terminate(Reason.MALFORMED_MSG);
				throw e;
			}
		}

		return validated;
	}

	@Override
	public byte[] put(final PCEPMessage msg) {
		return rawFactory.put(msg);
	}
}
