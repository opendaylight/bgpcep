/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import java.util.List;
import java.util.Map;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;

/**
 * Mock implementation of {@link BGPMessageParser}. It implements the required interface by having two internal maps,
 * each used in one of the methods. It looks up the key provided to the method and returns whatever value is stored in
 * the map.
 */
public class BGPMessageParserMock implements ProtocolMessageFactory<BGPMessage> {
	private final Map<byte[], List<BGPMessage>> messages;

	/**
	 * @param updateMessages Map<byte[], BGPUpdateEvent>
	 */
	public BGPMessageParserMock(final Map<byte[], List<BGPMessage>> messages) {
		this.messages = messages;
	}

	@Override
	public List<BGPMessage> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		final List<BGPMessage> ret = this.messages.get(bytes);
		if (ret == null) {
			throw new IllegalArgumentException("Undefined message encountered");
		}
		return ret;
	}

	@Override
	public byte[] put(final BGPMessage msg) {
		// nothing
		return null;
	}
}
