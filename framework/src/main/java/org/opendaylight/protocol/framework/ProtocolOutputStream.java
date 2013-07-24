/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Data stream interface between ProtocolMessage and byte array,
 * that represents this message in serialized form. Its instance
 * needs to be present in protocol specific session, to allow
 * sending messages from the session via the put() method.
 */
public final class ProtocolOutputStream {

	/**
	 * List of Buffers whose content needs to be written to socket.
	 */
	private final Queue<ByteBuffer> pendingData = new ArrayDeque<ByteBuffer>();

	/**
	 * Assumes that the message is valid (that you cannot create an invalid
	 * message from API). Serializes given messages to byte array, converts this
	 * byte array to byteBuffer and adds it to List.
	 * @param message message to be written
	 * @param factory protocol specific message factory
	 */
	public void putMessage(final ProtocolMessage message, final ProtocolMessageFactory factory) {
		final byte[] bytes = factory.put(message);
		if (bytes == null) {
			throw new IllegalArgumentException("Message parsed to null.");
		}
		synchronized (this.pendingData) {
			this.pendingData.add(ByteBuffer.wrap(bytes));
		}
	}

	/**
	 * Used by PCEPDispatcher to retrieve the data that needs to be written to
	 * socket.
	 *
	 * @return data that needs to be written to socket
	 */
	Queue<ByteBuffer> getBuffers() {
		return this.pendingData;
	}
}
