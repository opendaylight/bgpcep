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
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CClose;

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
	public PCEPObject parse(final byte[] bytes, final boolean processed, final boolean ignored) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		if (bytes.length != TLVS_F_OFFSET)
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + TLVS_F_OFFSET + "; Passed: "
					+ bytes.length);

		TerminationReason reason = TerminationReason.forValue((short) (bytes[REASON_F_OFFSET] & 0xFF));
		if (reason == null)
			reason = TerminationReason.Unknown;
		return new PCEPCloseObject(reason, PCEPTlvParser.parse(ByteArray.cutBytes(bytes, TLVS_F_OFFSET)));
	}

	@Override
	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof CClose))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPCloseObject.");

		final byte[] tlvs = PCEPTlvParser.put(((CClose) obj).getTlvs());
		final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length];
		ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);

		final int reason = ((CClose) obj).getReason().intValue();

		retBytes[REASON_F_OFFSET] = (byte) reason;

		return retBytes;
	}
}
