/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject
 * EROUnnumberedInterfaceSubobject}
 */
public class EROUnnumberedInterfaceSubobjectParser {
	public static final int ROUTER_ID_NUMBER_LENGTH = 4;
	public static final int INTERFACE_ID_NUMBER_LENGTH = 4;

	public static final int ROUTER_ID_NUMBER_OFFSET = 2; // added reserved field of size 2
	public static final int INTERFACE_ID_NUMBER_OFFSET = ROUTER_ID_NUMBER_OFFSET + ROUTER_ID_NUMBER_LENGTH;

	public static final int CONTENT_LENGTH = INTERFACE_ID_NUMBER_OFFSET + INTERFACE_ID_NUMBER_LENGTH;

	public static EROUnnumberedInterfaceSubobject parse(final byte[] soContentsBytes, final boolean loose) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		// return new EROUnnumberedInterfaceSubobject(new IPv4Address(ByteArray.subByte(soContentsBytes,
		// ROUTER_ID_NUMBER_OFFSET, ROUTER_ID_NUMBER_LENGTH)), new UnnumberedInterfaceIdentifier(
		// UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(soContentsBytes, INTERFACE_ID_NUMBER_OFFSET,
		// INTERFACE_ID_NUMBER_LENGTH)))), loose);
		return null;
	}

	public static byte[] put(final ExplicitRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof EROUnnumberedInterfaceSubobject))
			throw new IllegalArgumentException("Unknown ExplicitRouteSubobject instance. Passed " + objToSerialize.getClass()
					+ ". Needed EROUnnumberedInterfaceSubobject.");

		byte[] retBytes;
		retBytes = new byte[CONTENT_LENGTH];
		final EROUnnumberedInterfaceSubobject specObj = (EROUnnumberedInterfaceSubobject) objToSerialize;

		ByteArray.copyWhole(specObj.getRouterID().getAddress(), retBytes, ROUTER_ID_NUMBER_OFFSET);
		System.arraycopy(ByteArray.longToBytes(specObj.getInterfaceID().getInterfaceId()), Long.SIZE / Byte.SIZE
				- INTERFACE_ID_NUMBER_LENGTH, retBytes, INTERFACE_ID_NUMBER_OFFSET, INTERFACE_ID_NUMBER_LENGTH);

		return retBytes;
	}
}
