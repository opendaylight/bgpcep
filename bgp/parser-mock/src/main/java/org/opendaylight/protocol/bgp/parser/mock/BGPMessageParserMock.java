/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import java.io.IOException;
import java.util.Map;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPMessageParser;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessage;
import org.opendaylight.protocol.framework.ProtocolMessageHeader;

/**
 * Mock implementation of {@link BGPMessageParser}. It implements the required interface by having two internal maps,
 * each used in one of the methods. It looks up the key provided to the method and returns whatever value is stored in
 * the map.
 */
public class BGPMessageParserMock implements BGPMessageParser {
	private final Map<byte[], BGPMessage> messages;

	/**
	 * @param updateMessages Map<byte[], BGPUpdateEvent>
	 */
	public BGPMessageParserMock(final Map<byte[], BGPMessage> messages) {
		this.messages = messages;
	}

	@Override
	public void close() throws IOException {
		// nothing
	}

	@Override
	public BGPMessage parse(final byte[] bytes, final ProtocolMessageHeader msgHeader) throws DeserializerException, DocumentedException {
		final BGPMessage ret = this.messages.get(bytes);
		if (ret == null)
			throw new IllegalArgumentException("Undefined message encountered");
		return ret;
	}

	@Override
	public byte[] put(final ProtocolMessage msg) {
		// nothing
		return null;
	}
}
