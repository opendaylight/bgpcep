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
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPClassTypeObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPClassTypeObject PCEPClassTypeObject}
 */
public class PCEPClassTypeObjectParser implements PCEPObjectParser {

	/**
	 * Length of Class Type field in bits.
	 */
	public static final int CT_F_LENGTH = 3;

	/**
	 * Reserved field bit length.
	 */
	public static final int RESERVED = 29;

	/**
	 * Size of the object in bytes.
	 */
	public static final int SIZE = (RESERVED + CT_F_LENGTH) / 8;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored)
			throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");
		if (bytes.length != SIZE)
			throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE + "; Passed: " + bytes.length);
		if (!processed)
			throw new PCEPDocumentedException("Processed bit not set", PCEPErrors.P_FLAG_NOT_SET);
		final short classType = (short) (bytes[SIZE-1] & 0xFF);
		if (classType < 0 || classType > 8) {
			throw new PCEPDocumentedException("Invalid class type " + classType, PCEPErrors.INVALID_CT);
		}
		return new PCEPClassTypeObject(classType);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPClassTypeObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPClassTypeObject.");

		final byte[] retBytes = new byte[SIZE];
		retBytes[SIZE-1] = ByteArray.shortToBytes(((PCEPClassTypeObject) obj).getClassType())[1];
		return retBytes;
	}
}
