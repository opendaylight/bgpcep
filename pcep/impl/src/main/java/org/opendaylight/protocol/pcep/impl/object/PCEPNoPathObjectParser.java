/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvFactory;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPNoPathObject
 * PCEPNoPathObject}
 */
public class PCEPNoPathObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields in bytes
	 */
	public static final int NI_F_LENGTH = 1; //multi-field
	public static final int FLAGS_F_LENGTH = 2;
	public static final int RESERVED_F_LENGTH = 1;

	/*
	 * offsets of field in bytes
	 */

	public static final int NI_F_OFFSET = 0;
	public static final int FLAGS_F_OFFSET = NI_F_OFFSET + NI_F_LENGTH;
	public static final int RESERVED_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int TLVS_OFFSET = RESERVED_F_OFFSET + RESERVED_F_LENGTH;

	/*
	 * defined flags
	 */

	public static final int C_FLAG_OFFSET = 0;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, FLAGS_F_OFFSET, FLAGS_F_LENGTH));

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");

		return new PCEPNoPathObject((short) (bytes[NI_F_OFFSET] & 0xFF), flags.get(C_FLAG_OFFSET), PCEPTlvFactory.parse(ByteArray.cutBytes(bytes, TLVS_OFFSET)),
				ignored);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPNoPathObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPNoPathObject.");

		final PCEPNoPathObject nPObj = (PCEPNoPathObject) obj;

		final byte[] tlvs = PCEPTlvFactory.put(nPObj.getTlvs());
		final byte[] retBytes = new byte[tlvs.length + TLVS_OFFSET];
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(C_FLAG_OFFSET, nPObj.isConstrained());
		retBytes[NI_F_OFFSET] = ByteArray.shortToBytes(nPObj.getNatureOfIssue())[1];
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}

}
