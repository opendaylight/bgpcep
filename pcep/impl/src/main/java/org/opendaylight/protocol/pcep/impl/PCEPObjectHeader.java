/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.Arrays;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedBytes;

/**
 * Header parser for {@link org.opendaylight.protocol.pcep.PCEPObject PCEPObject}
 */
public class PCEPObjectHeader {

	private static final Logger logger = LoggerFactory.getLogger(PCEPObjectHeader.class);

	/*
	 * Common object header fields lengths in bytes
	 */
	public final static int OC_F_LENGTH = 1;
	public final static int OT_FLAGS_MF_LENGTH = 1; // multi-field
	public final static int OBJ_LENGTH_F_LENGTH = 2;

	/*
	 * size of fields inside of multi-filed in bits
	 */
	public final static int OT_SF_LENGTH = 4;
	public final static int FLAGS_SF_LENGTH = 4;

	/*
	 * offsets of fields inside of multi-field in bits
	 */
	public final static int OT_SF_OFFSET = 0;
	public final static int FLAGS_SF_OFFSET = OT_SF_OFFSET + OT_SF_LENGTH;

	/*
	 * flags offsets inside multi-filed
	 */
	public final static int P_FLAG_OFFSET = 6;
	public final static int I_FLAG_OFFSET = 7;

	/*
	 * Common object header fields offsets in bytes;
	 */
	public final static int OC_F_OFFSET = 0;
	public final static int OT_FLAGS_MF_OFFSET = OC_F_OFFSET + OC_F_LENGTH;
	public final static int OBJ_LENGTH_F_OFFSET = OT_FLAGS_MF_OFFSET + OT_FLAGS_MF_LENGTH;
	public final static int OBJ_BODY_OFFSET = OBJ_LENGTH_F_OFFSET + OBJ_LENGTH_F_LENGTH;

	/*
	 * Common object header length in bytes
	 */
	public final static int COMMON_OBJECT_HEADER_LENGTH = (OC_F_LENGTH + OT_FLAGS_MF_LENGTH + OBJ_LENGTH_F_LENGTH);

	public final int objClass;
	public final int objType;
	public final int objLength;
	public final boolean processed;
	public final boolean ignored;

	public PCEPObjectHeader(final int objClass, final int objType, final int objLength, final boolean processed, final boolean ignore) {
		this.objClass = objClass;
		this.objType = objType;
		this.objLength = objLength;
		this.processed = processed;
		this.ignored = ignore;

	}

	public static PCEPObjectHeader parseHeader(final byte[] bytes) {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory.");

		logger.trace("Attempt to parse object header from bytes: {}", ByteArray.bytesToHexString(bytes));

		final int objClass = ByteArray.bytesToInt(Arrays.copyOfRange(bytes, OC_F_OFFSET, OC_F_OFFSET + OC_F_LENGTH));

		final int objType = UnsignedBytes.toInt(ByteArray.copyBitsRange(bytes[OT_FLAGS_MF_OFFSET], OT_SF_OFFSET, OT_SF_LENGTH));

		final int objLength = ByteArray.bytesToInt(Arrays.copyOfRange(bytes, OBJ_LENGTH_F_OFFSET, OBJ_LENGTH_F_OFFSET + OBJ_LENGTH_F_LENGTH));

		final byte[] flagsBytes = { ByteArray.copyBitsRange(bytes[OT_FLAGS_MF_OFFSET], FLAGS_SF_OFFSET, FLAGS_SF_LENGTH) };

		final BitSet flags = ByteArray.bytesToBitSet(flagsBytes);

		final PCEPObjectHeader objHeader = new PCEPObjectHeader(objClass, objType, objLength, flags.get(P_FLAG_OFFSET), flags.get(I_FLAG_OFFSET));
		logger.trace("Object header was parsed. {}", objHeader);
		return objHeader;
	}

	public static byte[] putHeader(final PCEPObjectHeader header) {
		if (header == null)
			throw new IllegalArgumentException("PCEPObjectHeader is mandatory.");

		final byte[] retBytes = new byte[COMMON_OBJECT_HEADER_LENGTH];

		// objClass
		retBytes[OC_F_OFFSET] = (byte) header.objClass;

		// objType_flags multi-field
		retBytes[OT_FLAGS_MF_OFFSET] = (byte) (header.objType << (Byte.SIZE - OT_SF_LENGTH));
		if (header.processed)
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (P_FLAG_OFFSET) - 1;
		if (header.ignored)
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (I_FLAG_OFFSET) - 1;

		// objLength
		System.arraycopy(ByteArray.intToBytes(header.objLength), Integer.SIZE / Byte.SIZE - OBJ_LENGTH_F_LENGTH, retBytes, OBJ_LENGTH_F_OFFSET,
				OBJ_LENGTH_F_LENGTH);

		return retBytes;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPObjectHeader [objClass=");
		builder.append(this.objClass);
		builder.append(", objType=");
		builder.append(this.objType);
		builder.append(", objLength=");
		builder.append(this.objLength);
		builder.append(", processed=");
		builder.append(this.processed);
		builder.append(", ignored=");
		builder.append(this.ignored);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.ignored ? 1231 : 1237);
		result = prime * result + this.objClass;
		result = prime * result + this.objLength;
		result = prime * result + this.objType;
		result = prime * result + (this.processed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPObjectHeader other = (PCEPObjectHeader) obj;
		if (this.ignored != other.ignored)
			return false;
		if (this.objClass != other.objClass)
			return false;
		if (this.objLength != other.objLength)
			return false;
		if (this.objType != other.objType)
			return false;
		if (this.processed != other.processed)
			return false;
		return true;
	}

}
