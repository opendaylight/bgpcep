/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.framework.ProtocolMessageHeader;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedBytes;

/**
 * Representation of BGP Message Header. Includes methods for parsing and serializing header.
 * Length field represents the length of the message including the length of its header.
 */
public class BGPMessageHeader implements ProtocolMessageHeader {

	public static final Logger logger = LoggerFactory.getLogger(BGPMessageHeader.class);

	/**
	 * BGP message header fixed length (in bytes)
	 */
	public static final int COMMON_HEADER_LENGTH = 19;

	private static final int MARKER_SIZE = 16;
	private static final int LENGTH_SIZE = 2;
	private static final int TYPE_OFFSET = MARKER_SIZE + LENGTH_SIZE;

	private int type;
	private int length;
	private boolean parsed = false;

	/**
	 * Creates message header.
	 */
	public BGPMessageHeader() {

	}

	/**
	 * Creates message header with given attributes.
	 * @param type int
	 * @param length int
	 */
	public BGPMessageHeader(final int type, final int length) {
		this.type = type;
		this.length = length;
	}

	/**
	 * Creates a BGP Message Header from given byte array.
	 *
	 * @param bytes byte array to be parsed
	 * @return BGP Message Header
	 */
	public BGPMessageHeader fromBytes(final byte[] bytes) {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory");
		//FIXME: how to send Notification message from this point?
		//		final byte[] ones = new byte[MARKER_SIZE];
		//		Arrays.fill(ones, (byte)0xff);
		//
		//		if (Arrays.equals(bytes, ones))
		//			throw new BGPDocumentedException("Marker not set to ones.", BGPError.CONNECTION_NOT_SYNC);

		logger.trace("Attempt to parse message header: {}", ByteArray.bytesToHexString(bytes));

		if (bytes.length < COMMON_HEADER_LENGTH)
			throw new IllegalArgumentException("Too few bytes in passed array. Passed: " + bytes.length + ". Expected: >= " + COMMON_HEADER_LENGTH + ".");

		this.length = ByteArray.bytesToInt(ByteArray.subByte(bytes, MARKER_SIZE, LENGTH_SIZE));

		this.type = UnsignedBytes.toInt(bytes[TYPE_OFFSET]);

		logger.trace("Message header was parsed. {}", this);
		this.parsed = true;
		return this;
	}

	/**
	 * Serializes this BGP Message header to byte array.
	 *
	 * @return byte array representation of this header
	 */
	public byte[] toBytes() {
		final byte[] retBytes = new byte[COMMON_HEADER_LENGTH];

		Arrays.fill(retBytes, 0, MARKER_SIZE, (byte)0xff);

		System.arraycopy(ByteArray.intToBytes(this.length), Integer.SIZE / Byte.SIZE - LENGTH_SIZE, retBytes, MARKER_SIZE, LENGTH_SIZE);

		retBytes[TYPE_OFFSET] = (byte) this.type;

		return retBytes;
	}

	/**
	 * Returns length presented in Length field of this header.
	 *
	 * @return length of the BGP message
	 */
	public int getLength() {
		return this.length;
	}

	/**
	 * Returns type presented in Type field of this header.
	 *
	 * @return type of the BGP message
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Return the state of the parsing of this header.
	 *
	 * @return the parsed true if the header was parsed false if the header was not parsed
	 */
	public boolean isParsed() {
		return this.parsed;
	}

	/**
	 * Marks parsing as not finished. There is no need for other classes to mark parsing as done, therefore calling this
	 * method will set parsing to unfinished.
	 */
	public void setParsed() {
		this.parsed = false;
	}
}
