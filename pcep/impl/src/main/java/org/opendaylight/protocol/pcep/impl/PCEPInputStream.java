/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.io.IOException;
import java.io.PipedInputStream;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolInputStream;
import org.opendaylight.protocol.framework.ProtocolInputStreamFactory;
import org.opendaylight.protocol.framework.ProtocolMessage;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;

/**
 *
 */
public class PCEPInputStream implements ProtocolInputStream {
	static final ProtocolInputStreamFactory FACTORY = new ProtocolInputStreamFactory() {
		@Override
		public ProtocolInputStream getProtocolInputStream(final PipedInputStream pis, final ProtocolMessageFactory pmf) {
			return new PCEPInputStream(pis, pmf);
		}
	};

	private final ProtocolMessageFactory factory;
	private final PipedInputStream inputStream;
	private PCEPMessageHeader header;

	public PCEPInputStream(final PipedInputStream inputStream, final ProtocolMessageFactory factory) {
		this.factory = factory;
		this.inputStream = inputStream;
		this.header = new PCEPMessageHeader();
	}

	/**
	 * Check availability of a message in underlying input stream. A message is available when there are more or the
	 * same amount of bytes in the stream as the message length is specified in message header. If there are not enough
	 * bytes for the message or even to read a message header, return false.
	 *
	 * @return true if there are enough bytes to read a message false if there are not enough bytes to read a message or
	 *         a message header.
	 * @throws IOException
	 */
	@Override
	public synchronized boolean isMessageAvailable() throws IOException {
		if (!this.header.isParsed()) {
			// No header, try to parse it
			this.header = this.parseHeaderIfAvailable();

			if (!this.header.isParsed()) {
				// No luck, we do not have a message
				return false;
			}
		}
		// message length contains the size of the header too
		if (this.inputStream.available() < (this.header.getLength() - PCEPMessageHeader.COMMON_HEADER_LENGTH)) {
			return false;
		}
		return true;
	}

	/**
	 * If there are enough bytes in the underlying stream, parse the message. Blocking, till there are enough bytes to
	 * read, therefore the call of method isMessageAvailable() is suggested first.
	 *
	 * @param factory protocol specific message factory
	 * @return protocol specific message
	 * @throws DeserializerException
	 * @throws IOException
	 * @throws DocumentedException
	 */
	@Override
	public synchronized ProtocolMessage getMessage() throws DeserializerException, IOException, DocumentedException {
		// isMessageAvailable wasn't called, or there were not enough bytes to form message header
		// blocking till the header is available
		while (!this.header.isParsed()) {
			this.header = this.parseHeaderIfAvailable();
		}
		final byte[] bytes = new byte[this.header.getLength() - PCEPMessageHeader.COMMON_HEADER_LENGTH]; // message
																											// length
																											// contains
																											// the size
																											// of the
																											// header
																											// too
		// blocking till the whole message is available
		if (this.inputStream.read(bytes) == -1) {
			throw new IOException("PipedInputStream was closed, before data could be read from it.");
		}
		final ProtocolMessage msg = this.factory.parse(bytes, this.header);

		this.header.setParsed(); // if we have all the bytes to send the message for parsing, clear the header, to let know,
								// we're ready to read another message

		return msg;
	}

	/**
	 * Checks if there are enough bytes to parse a header and parses it. Non-blocking: if there are not enough bytes to
	 * parse a message header, returns false.
	 *
	 * @return cleared header if no header is available
	 * @return header object when enough data is available
	 */
	private PCEPMessageHeader parseHeaderIfAvailable() throws IOException {
		if (this.inputStream.available() < PCEPMessageHeader.COMMON_HEADER_LENGTH) {
			this.header.setParsed();
			return this.header;
		}

		final byte[] messageHeader = new byte[PCEPMessageHeader.COMMON_HEADER_LENGTH];
		if (this.inputStream.read(messageHeader) == -1) {
			this.header.setParsed();
			return this.header;
		}
		return this.header.fromBytes(messageHeader);
	}
}
