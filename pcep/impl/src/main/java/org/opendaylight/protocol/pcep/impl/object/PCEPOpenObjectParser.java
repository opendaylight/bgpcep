/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.OFListTlv;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPOpenObject PCEPOpenObject}
 */

public class PCEPOpenObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields in bytes
	 */
	public static final int VER_FLAGS_MF_LENGTH = 1; // multi-field
	public static final int KEEPALIVE_F_LENGTH = 1;
	public static final int DEAD_TIMER_LENGTH = 1;
	public static final int SID_F_LENGTH = 1;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	public static final int VERSION_SF_LENGTH = 3;
	public static final int FLAGS_SF_LENGTH = 5;

	/*
	 * offsets of field in bytes
	 */

	public static final int VER_FLAGS_MF_OFFSET = 0;
	public static final int KEEPALIVE_F_OFFSET = VER_FLAGS_MF_OFFSET + VER_FLAGS_MF_LENGTH;
	public static final int DEAD_TIMER_OFFSET = KEEPALIVE_F_OFFSET + KEEPALIVE_F_LENGTH;
	public static final int SID_F_OFFSET = DEAD_TIMER_OFFSET + DEAD_TIMER_LENGTH;
	public static final int TLVS_OFFSET = SID_F_OFFSET + SID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */

	public static final int VERSION_SF_OFFSET = 0;
	public static final int FLAGS_SF_OFFSET = VERSION_SF_LENGTH + VERSION_SF_OFFSET;

	public static final int PADDED_TO = 4;

	@Override
	public PCEPOpenObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");

		// parse version
		final int versionValue = ByteArray.copyBitsRange(bytes[VER_FLAGS_MF_OFFSET], VERSION_SF_OFFSET, VERSION_SF_LENGTH);

		if (versionValue != PCEPOpenObject.PCEP_VERSION)
			throw new PCEPDocumentedException("Unsupported PCEP version " + versionValue, PCEPErrors.PCEP_VERSION_NOT_SUPPORTED);

		final List<PCEPTlv> tlvs = PCEPTlvParser.parseTlv(ByteArray.cutBytes(bytes, TLVS_OFFSET));
		boolean ofListOccure = false;

		for (final PCEPTlv tlv : tlvs) {
			if (tlv instanceof OFListTlv) {
				if (ofListOccure)
					throw new PCEPDocumentedException("Invalid or unexpected message", PCEPErrors.NON_OR_INVALID_OPEN_MSG);

				ofListOccure = true;
			}
		}

		return new PCEPOpenObject(UnsignedBytes.toInt(bytes[KEEPALIVE_F_OFFSET]), UnsignedBytes.toInt(bytes[DEAD_TIMER_OFFSET]),
				UnsignedBytes.toInt(bytes[SID_F_OFFSET]), tlvs);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPOpenObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPOpenObject.");

		final PCEPOpenObject openObj = (PCEPOpenObject) obj;

		final byte versionFlagMF = (byte) (PCEPOpenObject.PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH));

		final byte[] tlvs = PCEPTlvParser.put(openObj.getTlvs());
		final byte[] bytes = new byte[TLVS_OFFSET + tlvs.length + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		// serialize version_flags multi-field
		bytes[VER_FLAGS_MF_OFFSET] = versionFlagMF;

		// serialize keepalive
		bytes[KEEPALIVE_F_OFFSET] = (byte) openObj.getKeepAliveTimerValue();

		// serialize dead timer
		bytes[DEAD_TIMER_OFFSET] = (byte) openObj.getDeadTimerValue();

		// serialize SID
		bytes[SID_F_OFFSET] = (byte) openObj.getSessionId();

		// serialize tlvs
		ByteArray.copyWhole(tlvs, bytes, TLVS_OFFSET);

		return bytes;
	}
}
