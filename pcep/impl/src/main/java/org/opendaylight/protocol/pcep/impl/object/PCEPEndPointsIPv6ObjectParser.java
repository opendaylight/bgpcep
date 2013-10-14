/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for IPv6 {@link org.opendaylight.protocol.pcep.object.PCEPEndPointsObject
 * PCEPEndPointsObject}
 */
public class PCEPEndPointsIPv6ObjectParser implements PCEPObjectParser {

	public static final int SRC6_F_LENGTH = 16;
	public static final int DEST6_F_LENGT = 16;

	public static final int SRC6_F_OFFSET = 0;
	public static final int DEST6_F_OFFSET = SRC6_F_OFFSET + SRC6_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory");
		if (bytes.length != SRC6_F_LENGTH + DEST6_F_LENGT)
			throw new PCEPDeserializerException("Wrong length of array of bytes.");

		if (!processed)
			throw new PCEPDocumentedException("Processed flag not set", PCEPErrors.P_FLAG_NOT_SET);

		return new PCEPEndPointsObject<IPv6Address>(
				new IPv6Address(ByteArray.subByte(bytes, SRC6_F_OFFSET, SRC6_F_LENGTH)),
				new IPv6Address(ByteArray.subByte(bytes, DEST6_F_OFFSET, DEST6_F_LENGT)));
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPEndPointsObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPEndPointsObject.");

		final PCEPEndPointsObject<?> ePObj = (PCEPEndPointsObject<?>) obj;

		if (!(ePObj.getSourceAddress() instanceof IPv6Address))
			throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + ePObj.getSourceAddress().getClass() + ". Needed IPv6Address");

		final byte[] retBytes = new byte[SRC6_F_LENGTH + DEST6_F_LENGT];
		ByteArray.copyWhole(((IPv6Address) ePObj.getSourceAddress()).getAddress(), retBytes, SRC6_F_OFFSET);
		ByteArray.copyWhole(((IPv6Address) ePObj.getDestinationAddress()).getAddress(), retBytes, DEST6_F_OFFSET);

		return retBytes;
	}

}
