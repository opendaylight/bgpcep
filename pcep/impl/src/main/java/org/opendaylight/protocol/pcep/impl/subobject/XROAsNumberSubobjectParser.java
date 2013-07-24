/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject
 * XROAsNumberSubobject}
 */

public class XROAsNumberSubobjectParser {
	public static final int AS_NUMBER_LENGTH = 2;

	public static final int AS_NUMBER_OFFSET = 0;

	public static final int CONTENT_LENGTH = AS_NUMBER_LENGTH + AS_NUMBER_OFFSET;

	public static XROAsNumberSubobject parse(byte[] soContentsBytes, boolean mandatory) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: " + CONTENT_LENGTH + ".");

		return new XROAsNumberSubobject(new ASNumber((ByteArray.bytesToShort(soContentsBytes) & 0xFFFF)), mandatory);
	}

	public static byte[] put(ExcludeRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof XROAsNumberSubobject))
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + objToSerialize.getClass() + ". Needed XROAsNumberSubobject.");

		final byte[] retBytes = new byte[CONTENT_LENGTH];

		System.arraycopy(ByteArray.longToBytes(((XROAsNumberSubobject) objToSerialize).getASNumber().getAsn()), Long.SIZE / Byte.SIZE - AS_NUMBER_LENGTH,
				retBytes, AS_NUMBER_OFFSET, AS_NUMBER_LENGTH);

		return retBytes;
	}
}
