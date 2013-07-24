/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.BitSet;

import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv RSVPErrorSpecTlv}
 * parameterized as IPv6Address
 */
public class RSVPErrorSpecIPv6TlvParser {

	private static final int IP_F_LENGTH = 16;
	private static final int FLAGS_F_LENGTH = 1;
	private static final int ERROR_CODE_F_LENGTH = 1;
	private static final int ERROR_VALUE_F_LENGTH = 2;

	private static final int IP_F_OFFSET = 0;
	private static final int FLAGS_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;
	private static final int ERROR_CODE_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	private static final int ERROR_VALUE_F_OFFSET = ERROR_CODE_F_OFFSET + ERROR_CODE_F_LENGTH;

	private static final int SIZE = ERROR_VALUE_F_OFFSET + ERROR_VALUE_F_LENGTH;

	/*
	 * flags offsets inside flags field in bits
	 */
	private static final int IN_PLACE_FLAG_OFFSET = 7;
	private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

	public static RSVPErrorSpecTlv<IPv6Address> parse(byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		if (valueBytes.length != SIZE) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: " + SIZE + ".");
		}

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(valueBytes, FLAGS_F_OFFSET, FLAGS_F_LENGTH));

		return new RSVPErrorSpecTlv<IPv6Address>(new IPv6Address(
				ByteArray.subByte(valueBytes, IP_F_OFFSET, IP_F_LENGTH)), flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET),
				valueBytes[ERROR_CODE_F_OFFSET] & 0xFF,
				ByteArray.bytesToShort(ByteArray.subByte(valueBytes, ERROR_VALUE_F_OFFSET, ERROR_VALUE_F_LENGTH)) & 0xFFFF);
	}

	public static byte[] put(RSVPErrorSpecTlv<?> objToSerialize) {
		if (objToSerialize == null)
			throw new IllegalArgumentException("RSVPErrorSpecTlv is mandatory.");

		if (!(((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress() instanceof IPv6Address))
			throw new IllegalArgumentException("Unknown parametrized type of RSVPErrorSpecTlv. Passed "
					+ ((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress().getClass() + ". Needed IPv6Address.");

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(IN_PLACE_FLAG_OFFSET, objToSerialize.isInPlace());
		flags.set(NOT_GUILTY_FLAGS_OFFSET, objToSerialize.isGuilty());

		final byte[] retBytes = new byte[SIZE];

		ByteArray.copyWhole(((IPv6Address) objToSerialize.getErrorNodeAddress()).getAddress(), retBytes, IP_F_OFFSET);
		retBytes[ERROR_CODE_F_OFFSET] = ByteArray.intToBytes(objToSerialize.getErrorCode())[Integer.SIZE / Byte.SIZE - 1];
		System.arraycopy(ByteArray.intToBytes(objToSerialize.getErrorValue()), Integer.SIZE / Byte.SIZE - ERROR_VALUE_F_LENGTH, retBytes, ERROR_VALUE_F_OFFSET,
				ERROR_VALUE_F_LENGTH);
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

		return retBytes;
	}

}
