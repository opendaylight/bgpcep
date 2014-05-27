/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import java.util.BitSet;

import org.opendaylight.protocol.util.ByteArray;

import com.google.common.primitives.UnsignedBytes;

public final class LabelUtil {

	private static final int RES_F_LENGTH = 1;

	private static final int C_TYPE_F_LENGTH = 1;

	private static final int RES_F_OFFSET = 0;

	private static final int C_TYPE_F_OFFSET = RES_F_OFFSET + RES_F_LENGTH;

	private static final int HEADER_OFFSET = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

	private static final int U_FLAG_OFFSET = 0;

	private static final int G_FLAG_OFFSET = 7;

	private LabelUtil() {
		throw new UnsupportedOperationException("Utility class should not be instantiated");
	}

	public static byte[] formatLabel(final int type, final boolean unidirectional, final boolean global, final byte[] labelbytes) {

		final byte[] retBytes = new byte[labelbytes.length + HEADER_OFFSET];

		System.arraycopy(labelbytes, 0, retBytes, HEADER_OFFSET, labelbytes.length);

		final BitSet reserved = new BitSet();
		reserved.set(U_FLAG_OFFSET, unidirectional);
		reserved.set(G_FLAG_OFFSET, global);
		System.arraycopy(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH), 0, retBytes, RES_F_OFFSET, RES_F_LENGTH);
		retBytes[C_TYPE_F_OFFSET] = UnsignedBytes.checkedCast(type);
		return retBytes;
	}
}
