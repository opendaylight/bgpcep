/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;

/**
 * Data stream interface between Protocol Message and byte array, that represents this message as Java object. Used by
 * the Dispatcher.
 */
public interface ProtocolInputStream {

	/**
	 * Check availability of a message in underlying input stream. A message is available when there are more or the
	 * same amount of bytes in the stream as the message length is specified in message header. If there are not enough
	 * bytes for the message or even to read a message header, return false. Needs to be synchronized.
	 *
	 * @return true if there are enough bytes to read a message false if there are not enough bytes to read a message or
	 *         a message header.
	 *
	 * @throws IOException this exception may be thrown when "impossible" protocol buffering conditions occur.
	 *         Examples include: we are attempting to wait for more data than is theoretically possible (e.g. framing
	 *         error), peer is attempting to make us buffer more data than possible to accomodate (2G chunk), etc.
	 */
	public boolean isMessageAvailable() throws IOException;

	/**
	 * If there are enough bytes in the underlying stream, parse the message. Blocking, till there are enough bytes to
	 * read, therefore the call of method isMessageAvailable() is suggested first. Needs to be synchronized.
	 *
	 * @return protocol specific message
	 *
	 * @throws DeserializerException if the parsing was not successful due to syntax error
	 * @throws IOException if there was problem with extracting bytes from the stream
	 * @throws DocumentedException if the parsing was not successful
	 */
	public ProtocolMessage getMessage() throws DeserializerException, IOException, DocumentedException;
}
