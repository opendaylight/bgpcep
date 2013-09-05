/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.Arrays;

import org.opendaylight.protocol.framework.ProtocolMessageHeader;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Header parser for {@link org.opendaylight.protocol.pcep.PCEPMessage PCEPMessage}
 */
public final class PCEPMessageHeader implements ProtocolMessageHeader {

	public static final Logger logger = LoggerFactory.getLogger(PCEPMessageHeader.class);

	/*
	 * lengths of fields in bytes
	 */
	private static final int VER_FLAGS_MF_LENGTH = 1;
	private static final int TYPE_F_LENGTH = 1;
	private static final int LENGTH_F_LENGTH = 2;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	private static final int VERSION_SF_LENGTH = 3;

	/*
	 * offsets of field in bytes
	 */
	private static final int VER_FLAGS_MF_OFFSET = 0;
	private static final int TYPE_F_OFFSET = VER_FLAGS_MF_LENGTH + VER_FLAGS_MF_OFFSET;
	private static final int LENGTH_F_OFFSET = TYPE_F_LENGTH + TYPE_F_OFFSET;

	/*
	 * offsets of subfields inside multi-filed in bits
	 */

	private static final int VERSION_SF_OFFSET = 0;

	/*
	 * COMMON HEADER LENGTH
	 */
	public static final int COMMON_HEADER_LENGTH = VER_FLAGS_MF_LENGTH + TYPE_F_LENGTH + LENGTH_F_LENGTH;

	private final int type;
	private final int length;
	private final int version;

	public PCEPMessageHeader(final int type, final int length, final int version) {
		this.type = type;
		this.length = length;
		this.version = version;
	}

	public static PCEPMessageHeader fromBytes(final byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("Array of bytes is mandatory");
		}

		logger.trace("Attempt to parse message header: {}", ByteArray.bytesToHexString(bytes));

		if (bytes.length < COMMON_HEADER_LENGTH) {
			throw new IllegalArgumentException("Too few bytes in passed array. Passed: " + bytes.length + "; Expected: >= " + COMMON_HEADER_LENGTH + ".");
		}

		final int type = UnsignedBytes.toInt(bytes[TYPE_F_OFFSET]);

		final int length = ByteArray.bytesToInt(Arrays.copyOfRange(bytes,
				LENGTH_F_OFFSET, LENGTH_F_OFFSET + LENGTH_F_LENGTH));

		final int version = ByteArray.copyBitsRange(bytes[VER_FLAGS_MF_OFFSET], VERSION_SF_OFFSET, VERSION_SF_LENGTH);

		final PCEPMessageHeader ret = new PCEPMessageHeader(type, length, version);

		logger.trace("Message header was parsed. {}", ret);
		return ret;
	}

	public byte[] toBytes() {
		final byte[] retBytes = new byte[COMMON_HEADER_LENGTH];

		// msgVer_Flag
		retBytes[VER_FLAGS_MF_OFFSET] = (byte) (this.version << (Byte.SIZE - VERSION_SF_LENGTH));

		// msgType
		retBytes[TYPE_F_OFFSET] = (byte) this.type;

		// msgLength
		System.arraycopy(ByteArray.intToBytes(this.length), Integer.SIZE / Byte.SIZE - LENGTH_F_LENGTH, retBytes, LENGTH_F_OFFSET, LENGTH_F_LENGTH);

		return retBytes;
	}

	public int getLength() {
		return this.length;
	}

	public int getVersion() {
		return this.version;
	}

	public int getType() {
		return this.type;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPMessageHeader [type=");
		builder.append(this.type);
		builder.append(", length=");
		builder.append(this.length);
		builder.append(", version=");
		builder.append(this.version);
		builder.append("]");
		return builder.toString();
	}
}
