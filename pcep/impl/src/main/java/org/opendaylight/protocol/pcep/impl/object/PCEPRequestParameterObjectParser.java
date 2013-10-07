/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject
 * PCEPRequestParameterObject}
 */

public class PCEPRequestParameterObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields in bytes
	 */
	public static final int FLAGS_PRI_MF_LENGTH = 4; //multi-field
	public static final int RID_F_LENGTH = 4;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	public static final int FLAGS_SF_LENGTH = 29;
	public static final int PRI_SF_LENGTH = 3;

	/*
	 * offsets of field in bytes
	 */

	public static final int FLAGS_PRI_MF_OFFSET = 0;
	public static final int RID_F_OFFSET = FLAGS_PRI_MF_OFFSET + FLAGS_PRI_MF_LENGTH;
	public static final int TLVS_OFFSET = RID_F_OFFSET + RID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */

	public static final int FLAGS_SF_OFFSET = 0;
	public static final int PRI_SF_OFFSET = FLAGS_SF_OFFSET + FLAGS_SF_LENGTH;

	/*
	 * flags offsets inside flags sub-field in bits
	 */

	private static final int O_FLAG_OFFSET = 26;
	private static final int B_FLAG_OFFSET = 27;
	private static final int R_FLAG_OFFSET = 28;

	/*
	 * GCO extension flags offsets inside flags sub-field in bits
	 */
	private static final int M_FLAG_OFFSET = 21;
	private static final int D_FLAG_OFFSET = 22;

	/*
	 * OF extension flags offsets inside flags sub.field in bits
	 */

	private static int S_FLAG_OFFSET = 24; //Supply OF on response

	/*
	 * RFC6006 flags
	 */
	private static int F_FLAG_OFFSET = 18;

	private static int N_FLAG_OFFSET = 19;

	private static int E_FLAG_OFFSET = 20;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");

		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(bytes, FLAGS_PRI_MF_OFFSET, FLAGS_PRI_MF_OFFSET + FLAGS_PRI_MF_LENGTH));
		short priority = 0;
		priority |= flags.get(PRI_SF_OFFSET + 2) ? 1 : 0;
		priority |= (flags.get(PRI_SF_OFFSET + 1) ? 1 : 0) << 1;
		priority |= (flags.get(PRI_SF_OFFSET) ? 1 : 0) << 2;

		return new PCEPRequestParameterObject(flags.get(O_FLAG_OFFSET), flags.get(B_FLAG_OFFSET), flags.get(R_FLAG_OFFSET), flags.get(M_FLAG_OFFSET),
				flags.get(D_FLAG_OFFSET), flags.get(S_FLAG_OFFSET), flags.get(F_FLAG_OFFSET), flags.get(N_FLAG_OFFSET), flags.get(E_FLAG_OFFSET), priority,
				ByteArray.bytesToLong(Arrays.copyOfRange(bytes, RID_F_OFFSET, RID_F_OFFSET + RID_F_LENGTH)), PCEPTlvParser.parseTlv(ByteArray.cutBytes(bytes,
						TLVS_OFFSET)), processed, ignored);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPRequestParameterObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPRequestParameterObject.");

		final PCEPRequestParameterObject rPObj = (PCEPRequestParameterObject) obj;

		final BitSet flags_priority = new BitSet(FLAGS_PRI_MF_LENGTH * Byte.SIZE);

		flags_priority.set(R_FLAG_OFFSET, rPObj.isReoptimized());
		flags_priority.set(B_FLAG_OFFSET, rPObj.isBidirectional());
		flags_priority.set(O_FLAG_OFFSET, rPObj.isLoose());
		flags_priority.set(M_FLAG_OFFSET, rPObj.isMakeBeforeBreak());
		flags_priority.set(D_FLAG_OFFSET, rPObj.isReportRequestOrder());
		flags_priority.set(S_FLAG_OFFSET, rPObj.isSuplyOFOnResponse());
		flags_priority.set(F_FLAG_OFFSET, rPObj.isFragmentation());
		flags_priority.set(N_FLAG_OFFSET, rPObj.isP2mp());
		flags_priority.set(E_FLAG_OFFSET, rPObj.isEroCompression());

		flags_priority.set(PRI_SF_OFFSET, (rPObj.getPriority() & 1 << 2) != 0);
		flags_priority.set(PRI_SF_OFFSET + 1, (rPObj.getPriority() & 1 << 1) != 0);
		flags_priority.set(PRI_SF_OFFSET + 2, (rPObj.getPriority() & 1) != 0);

		final byte[] tlvs = PCEPTlvParser.put(rPObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags_priority, FLAGS_PRI_MF_LENGTH), retBytes, FLAGS_PRI_MF_OFFSET);
		ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(rPObj.getRequestID()), (Long.SIZE / Byte.SIZE) - RID_F_LENGTH, RID_F_LENGTH), retBytes,
				RID_F_OFFSET);

		return retBytes;
	}

}
