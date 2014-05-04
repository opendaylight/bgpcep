/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.util.ByteArray;

public final class TlvUtil {

	private static final int TLV_TYPE_F_LENGTH = 2;
	private static final int TLV_LENGTH_F_LENGTH = 2;
	private static final int HEADER_SIZE = TLV_LENGTH_F_LENGTH + TLV_TYPE_F_LENGTH;

	protected static final int PADDED_TO = 4;

	private TlvUtil() {
		throw new UnsupportedOperationException("Utility class should not be instantiated");
	}

	public static byte[] formatTlv(final int type, final byte[] valueBytes) {
		final byte[] typeBytes = ByteArray.intToBytes(type, TLV_TYPE_F_LENGTH);

		final byte[] lengthBytes = ByteArray.intToBytes(valueBytes.length, TLV_LENGTH_F_LENGTH);

		final byte[] bytes = new byte[HEADER_SIZE + valueBytes.length + AbstractObjectWithTlvsParser.getPadding(HEADER_SIZE + valueBytes.length, PADDED_TO)];

		int byteOffset = 0;
		System.arraycopy(typeBytes, 0, bytes, byteOffset, TLV_TYPE_F_LENGTH);
		byteOffset += TLV_TYPE_F_LENGTH;
		System.arraycopy(lengthBytes, 0, bytes, byteOffset, TLV_LENGTH_F_LENGTH);
		byteOffset += TLV_LENGTH_F_LENGTH;
		System.arraycopy(valueBytes, 0, bytes, byteOffset, valueBytes.length);
		return bytes;
	}
}
