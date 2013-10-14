/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSRLGSubobject;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject. XROSRLGSubobject XROSRLGSubobject}
 */
public class XROSRLGSubobjectParser {
	public static final int SRLG_ID_NUMBER_LENGTH = 4;
	public static final int ATTRIBUTE_LENGTH = 1;

	public static final int SRLG_ID_NUMBER_OFFSET = 0;
	public static final int ATTRIBUTE_OFFSET = SRLG_ID_NUMBER_OFFSET + SRLG_ID_NUMBER_LENGTH + 1; // added reserved
																									// field of size 1

	public static final int CONTENT_LENGTH = ATTRIBUTE_OFFSET + ATTRIBUTE_LENGTH;

	public static XROSRLGSubobject parse(final byte[] soContentsBytes, final boolean mandatory) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		// return new XROSRLGSubobject(new
		// SharedRiskLinkGroup(UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(soContentsBytes,
		// SRLG_ID_NUMBER_OFFSET,
		// SRLG_ID_NUMBER_LENGTH)))), mandatory);
		return null;
	}

	public static byte[] put(final ExcludeRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof XROSRLGSubobject))
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + objToSerialize.getClass()
					+ ". Needed XROSRLGSubobject.");

		byte[] retBytes;
		retBytes = new byte[CONTENT_LENGTH];
		final XROSRLGSubobject specObj = (XROSRLGSubobject) objToSerialize;

		// ByteArray.copyWhole(ByteArray.intToBytes((int) specObj.getSrlgId().getValue()), retBytes,
		// SRLG_ID_NUMBER_OFFSET);
		retBytes[ATTRIBUTE_OFFSET] = (byte) XROSubobjectAttributeMapping.getInstance().getFromAttributeEnum(specObj.getAttribute());

		return retBytes;
	}
}
