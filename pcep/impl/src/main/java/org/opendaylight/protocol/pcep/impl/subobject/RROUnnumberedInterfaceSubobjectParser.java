/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.RROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.RROUnnumberedInterfaceSubobject
 * RROUnnumberedInterfaceSubobject}
 */
public class RROUnnumberedInterfaceSubobjectParser {
	public static final int ROUTER_ID_NUMBER_LENGTH = 4;
	public static final int INTERFACE_ID_NUMBER_LENGTH = 4;

	public static final int ROUTER_ID_NUMBER_OFFSET = 2; // added reserved field of size 2
	public static final int INTERFACE_ID_NUMBER_OFFSET = ROUTER_ID_NUMBER_OFFSET + ROUTER_ID_NUMBER_LENGTH;

	public static final int CONTENT_LENGTH = INTERFACE_ID_NUMBER_OFFSET + INTERFACE_ID_NUMBER_LENGTH;

	public static RROUnnumberedInterfaceSubobject parse(final byte[] soContentsBytes) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		// return new RROUnnumberedInterfaceSubobject(new IPv4Address(
		// ByteArray.subByte(soContentsBytes, ROUTER_ID_NUMBER_OFFSET, ROUTER_ID_NUMBER_LENGTH)), new
		// UnnumberedInterfaceIdentifier(
		// UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(soContentsBytes, INTERFACE_ID_NUMBER_OFFSET,
		// INTERFACE_ID_NUMBER_LENGTH)))));
		return null;
	}

	public static byte[] put(final ReportedRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof RROUnnumberedInterfaceSubobject))
			throw new IllegalArgumentException("Unknown ReportedRouteSubobject instance. Passed " + objToSerialize.getClass()
					+ ". Needed RROUnnumberedInterfaceSubobject.");

		byte[] retBytes;
		retBytes = new byte[CONTENT_LENGTH];
		final RROUnnumberedInterfaceSubobject specObj = (RROUnnumberedInterfaceSubobject) objToSerialize;

		ByteArray.copyWhole(specObj.getRouterID().getValue().getBytes(), retBytes, ROUTER_ID_NUMBER_OFFSET);
		System.arraycopy(ByteArray.longToBytes(specObj.getInterfaceID().getInterfaceId()), Long.SIZE / Byte.SIZE
				- INTERFACE_ID_NUMBER_LENGTH, retBytes, INTERFACE_ID_NUMBER_OFFSET, INTERFACE_ID_NUMBER_LENGTH);

		return retBytes;
	}
}
