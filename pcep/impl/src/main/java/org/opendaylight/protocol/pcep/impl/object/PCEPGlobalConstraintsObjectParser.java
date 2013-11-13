/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvFactory;
import org.opendaylight.protocol.pcep.object.PCEPGlobalConstraintsObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPGlobalConstraintsObject
 * PCEPGlobalConstraints}
 */
public class PCEPGlobalConstraintsObjectParser implements PCEPObjectParser {

	private final static int MAX_HOP_F_LENGTH = 1;
	private final static int MAX_UTIL_F_LENGTH = 1;
	private final static int MIN_UTIL_F_LENGTH = 1;
	private final static int OVER_BOOKING_FACTOR_F_LENGTH = 1;

	private final static int MAX_HOP_F_OFFSET = 0;
	private final static int MAX_UTIL_F_OFFSET = MAX_HOP_F_OFFSET + MAX_HOP_F_LENGTH;
	private final static int MIN_UTIL_F_OFFSET = MAX_UTIL_F_OFFSET + MAX_UTIL_F_LENGTH;
	private final static int OVER_BOOKING_FACTOR_F_OFFSET = MIN_UTIL_F_OFFSET + MIN_UTIL_F_LENGTH;

	private final static int TLVS_OFFSET = OVER_BOOKING_FACTOR_F_OFFSET + OVER_BOOKING_FACTOR_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");

		return new PCEPGlobalConstraintsObject((short) (bytes[MAX_HOP_F_OFFSET] & 0xFF), (short) (bytes[MAX_UTIL_F_OFFSET] & 0xFF),
				(short) (bytes[MIN_UTIL_F_OFFSET] & 0xFF), (short) (bytes[OVER_BOOKING_FACTOR_F_OFFSET] & 0xFF), PCEPTlvFactory.parse(ByteArray.cutBytes(bytes,
						TLVS_OFFSET)), processed, ignored);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPGlobalConstraintsObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPGlobalConstraints.");

		final PCEPGlobalConstraintsObject specObj = (PCEPGlobalConstraintsObject) obj;

		final byte[] tlvs = PCEPTlvFactory.put(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];

		retBytes[MAX_HOP_F_OFFSET] = (byte) specObj.getMaxHop();
		retBytes[MAX_UTIL_F_OFFSET] = (byte) specObj.getMaxUtilization();
		retBytes[MIN_UTIL_F_OFFSET] = (byte) specObj.getMinUtilization();
		retBytes[OVER_BOOKING_FACTOR_F_OFFSET] = (byte) specObj.getOverBookingFactor();

		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}

}
