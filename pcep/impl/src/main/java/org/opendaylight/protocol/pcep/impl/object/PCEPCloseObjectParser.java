/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPCloseObject PCEPCloseObject}
 */
public class PCEPCloseObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields in bytes
	 */
	public static final int FLAGS_F_LENGTH = 1;
	public static final int REASON_F_LENGTH = 1;

	/*
	 * offsets of fields in bytes
	 */
	public static final int FLAGS_F_OFFSET = 2; // added reserved field of size 2 bytes
	public static final int REASON_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

	/*
	 * total size of object in bytes
	 */
	public static final int TLVS_F_OFFSET = REASON_F_OFFSET + REASON_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		if (bytes.length != TLVS_F_OFFSET)
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + TLVS_F_OFFSET + "; Passed: " + bytes.length);

		Reason reason;
		switch ((short) (bytes[REASON_F_OFFSET] & 0xFF)) {
			case 1:
				reason = Reason.UNKNOWN;
				break;
			case 2:
				reason = Reason.EXP_DEADTIMER;
				break;
			case 3:
				reason = Reason.MALFORMED_MSG;
				break;
			case 4:
				reason = Reason.TOO_MANY_UNKNOWN_REQ_REP;
				break;
			case 5:
				reason = Reason.TOO_MANY_UNKNOWN_MSG;
				break;
			default:
				reason = Reason.UNKNOWN;
				break;
		}

		return new PCEPCloseObject(reason, PCEPTlvParser.parseTlv(ByteArray.cutBytes(bytes, TLVS_F_OFFSET)));
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPCloseObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPCloseObject.");

		final byte[] tlvs = PCEPTlvParser.put(((PCEPCloseObject) obj).getTlvs());
		final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length];
		ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);

		int reason;
		switch (((PCEPCloseObject) obj).getReason()) {
			case UNKNOWN:
				reason = 1;
				break;
			case EXP_DEADTIMER:
				reason = 2;
				break;
			case MALFORMED_MSG:
				reason = 3;
				break;
			case TOO_MANY_UNKNOWN_REQ_REP:
				reason = 4;
				break;
			case TOO_MANY_UNKNOWN_MSG:
				reason = 5;
				break;
			default:
				reason = 1;
				break;
		}

		retBytes[REASON_F_OFFSET] = (byte) reason;

		return retBytes;
	}

}
