/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.protocol.util.ByteArray;

import com.google.common.primitives.UnsignedBytes;

public final class ObjectUtil {

	private static final int HEADER_SIZE = 4;

	private static final int OC_F_LENGTH = 1;
	private static final int OT_FLAGS_MF_LENGTH = 1;
	private static final int OBJ_LENGTH_F_LENGTH = 2;

	private static final int OC_F_OFFSET = 0;
	private static final int OT_FLAGS_MF_OFFSET = OC_F_OFFSET + OC_F_LENGTH;
	private static final int OBJ_LENGTH_F_OFFSET = OT_FLAGS_MF_OFFSET + OT_FLAGS_MF_LENGTH;

	private static final int OT_SF_LENGTH = 4;
	/*
	 * flags offsets inside multi-filed
	 */
	private static final int P_FLAG_OFFSET = 6;
	private static final int I_FLAG_OFFSET = 7;

	private ObjectUtil() {
	}

	public static byte[] formatSubobject(final int objectType, final int objectClass, final Boolean processingRule, final Boolean ignore,
			final byte[] valueBytes) {

		final byte[] bytes = new byte[HEADER_SIZE + valueBytes.length];

		// objClass
		bytes[0] = UnsignedBytes.checkedCast(objectClass);

		// objType_flags multi-field
		bytes[OT_FLAGS_MF_OFFSET] = UnsignedBytes.checkedCast(objectType << (Byte.SIZE - OT_SF_LENGTH));
		if (processingRule != null && processingRule) {
			bytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (P_FLAG_OFFSET) - 1;
		}
		if (ignore != null && ignore) {
			bytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (I_FLAG_OFFSET) - 1;
		}

		// objLength
		System.arraycopy(ByteArray.intToBytes(valueBytes.length + HEADER_SIZE, OBJ_LENGTH_F_LENGTH), 0, bytes, OBJ_LENGTH_F_OFFSET,
				OBJ_LENGTH_F_LENGTH);
		System.arraycopy(valueBytes, 0, bytes, HEADER_SIZE, valueBytes.length);
		return bytes;
	}
}
