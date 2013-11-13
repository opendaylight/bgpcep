/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPOFCodesMapping;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPTlvFactory;
import org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject
 * PCEPObjectiveFunctionObject}
 */
public class PCEPObjectiveFunctionObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields
	 */
	public static final int OF_CODE_F_LENGTH = 2;

	/*
	 * offsets of fields
	 */
	public static final int OF_CODE_F_OFFSET = 0;
	public static final int TLVS_OFFSET = OF_CODE_F_OFFSET + OF_CODE_F_LENGTH + 2; // added reserved field of size 2

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");
		try {
			return new PCEPObjectiveFunctionObject(PCEPOFCodesMapping.getInstance().getFromCodeIdentifier(
					ByteArray.bytesToShort(ByteArray.subByte(bytes, OF_CODE_F_OFFSET, OF_CODE_F_LENGTH)) & 0xFFFF), PCEPTlvFactory.parse(ByteArray.cutBytes(
					bytes, TLVS_OFFSET)), processed, ignored);
		} catch (final NoSuchElementException e) {
			throw new PCEPDeserializerException(e, "Objective function object has unknown identifier.");
		}
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPObjectiveFunctionObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPObjectiveFunction.");

		final PCEPObjectiveFunctionObject specObj = (PCEPObjectiveFunctionObject) obj;

		final byte[] tlvs = PCEPTlvFactory.put(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];

		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		ByteArray.copyWhole(ByteArray.shortToBytes((short) PCEPOFCodesMapping.getInstance().getFromOFCodesEnum(specObj.getCode())), retBytes, OF_CODE_F_OFFSET);

		return retBytes;
	}

}
