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
import org.opendaylight.protocol.pcep.spi.RawMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.base.Preconditions;

/**
 * A PCEP message parser which also does validation.
 */
public final class PCEPMessageFactory implements ProtocolMessageFactory<Message> {
	private static final RawPCEPMessageFactory rawFactory = new RawPCEPMessageFactory();

	@Override
	public List<Message> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		final List<Message> parsed = rawFactory.parse(bytes);
		final List<Message> validated = new ArrayList<>(parsed.size());

		for (final Message msg : parsed) {
			Preconditions.checkState(msg instanceof RawMessage);
			final RawMessage raw = (RawMessage) msg;

			// try {
			// validated.addAll(PCEPMessageValidator.getValidator(raw.getMsgType()).validate(raw.getAllObjects()));
			// } catch (final PCEPDeserializerException e) {
			// // FIXME: at validation time we may want to terminate with:
			// // logger.error("Malformed message, terminating. ", e);
			// // this.terminate(Reason.MALFORMED_MSG);
			// throw e;
			// }
		}

		return validated;
	}

	@Override
	public byte[] put(final Message msg) {
		return rawFactory.put(msg);
	}
}
