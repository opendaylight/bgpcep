/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for IPv4 {@link org.opendaylight.protocol.pcep.object.PCEPEndPointsObject
 * PCEPEndPointsObject}
 */
public class PCEPEndPointsIPv4ObjectParser implements PCEPObjectParser {

	/*
	 * fields lengths and offsets for IPv4 in bytes
	 */
	public static final int SRC4_F_LENGTH = 4;
	public static final int DEST4_F_LENGTH = 4;

	public static final int SRC4_F_OFFSET = 0;
	public static final int DEST4_F_OFFSET = SRC4_F_OFFSET + SRC4_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory");
		if (bytes.length != SRC4_F_LENGTH + DEST4_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes.");

		//FIXME: create new constructor which allows processed parameter - needed for validation
		return new PCEPEndPointsObject<IPv4Address>(
				new IPv4Address(ByteArray.subByte(bytes, SRC4_F_OFFSET, SRC4_F_LENGTH)),
				new IPv4Address(ByteArray.subByte(bytes, DEST4_F_OFFSET, DEST4_F_LENGTH)));
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPEndPointsObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPEndPointsObject.");

		final PCEPEndPointsObject<?> ePObj = (PCEPEndPointsObject<?>) obj;

		if (!(ePObj.getSourceAddress() instanceof IPv4Address))
			throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + ePObj.getSourceAddress().getClass() + ". Needed IPv4Address");

		final byte[] retBytes = new byte[SRC4_F_LENGTH + DEST4_F_LENGTH];
		ByteArray.copyWhole(((IPv4Address) ePObj.getSourceAddress()).getAddress(), retBytes, SRC4_F_OFFSET);
		ByteArray.copyWhole(((IPv4Address) ePObj.getDestinationAddress()).getAddress(), retBytes, DEST4_F_OFFSET);

		return retBytes;
	}
}
