/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.mock;

import java.util.Map;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Mock implementation of {@link BGPMessageParser}. It implements the required interface by having two internal maps,
 * each used in one of the methods. It looks up the key provided to the method and returns whatever value is stored in
 * the map.
 */
public class BGPMessageParserMock implements ProtocolMessageFactory<Notification> {
	private final Map<byte[], Notification> messages;

	/**
	 * @param updateMessages Map<byte[], BGPUpdateEvent>
	 */
	public BGPMessageParserMock(final Map<byte[], Notification> messages) {
		this.messages = messages;
	}

	@Override
	public Notification parse(final byte[] bytes) throws BGPParsingException, BGPDocumentedException {
		final Notification ret = this.messages.get(bytes);
		if (ret == null) {
			throw new IllegalArgumentException("Undefined message encountered");
		}
		return ret;
	}

	@Override
	public byte[] put(final Notification msg) {
		// nothing
		return null;
	}
}
