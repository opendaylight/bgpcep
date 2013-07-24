/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject
 * EROIPPrefixSubobject<IPv6Prefix>}
 */
public class EROIPv6PrefixSubobjectParser {
	public static final int IP_F_LENGTH = 16;
	public static final int PREFIX_F_LENGTH = 1;

	public static final int IP_F_OFFSET = 0;
	public static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;

	public static final int CONTENT_LENGTH = PREFIX_F_OFFSET + PREFIX_F_LENGTH + 1; // added reserved field of size 1 byte

	public static EROIPPrefixSubobject<IPv6Prefix> parse(byte[] soContentsBytes, boolean loose) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: " + CONTENT_LENGTH + ".");

		final IPv6Address address = new IPv6Address(ByteArray.subByte(soContentsBytes, IP_F_OFFSET, IP_F_LENGTH));
		final int length = UnsignedBytes.toInt(soContentsBytes[PREFIX_F_OFFSET]);

		return new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(address, length), loose);
	}

	public static byte[] put(ExplicitRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof EROIPPrefixSubobject))
			throw new IllegalArgumentException("Unknown ExplicitRouteSubobject instance. Passed " + objToSerialize.getClass()
					+ ". Needed EROIPPrefixSubobject.");

		final EROIPPrefixSubobject<?> specObj = (EROIPPrefixSubobject<?>) objToSerialize;
		final Prefix<?> prefix = specObj.getPrefix();

		if (!(prefix instanceof IPv6Prefix))
			throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ". Needed IPv6Prefix.");

		final byte[] retBytes = new byte[CONTENT_LENGTH];

		ByteArray.copyWhole(prefix.getAddress().getAddress(), retBytes, IP_F_OFFSET);
		retBytes[PREFIX_F_OFFSET] = ByteArray.intToBytes(prefix.getLength())[Integer.SIZE / Byte.SIZE - 1];

		return retBytes;
	}
}
