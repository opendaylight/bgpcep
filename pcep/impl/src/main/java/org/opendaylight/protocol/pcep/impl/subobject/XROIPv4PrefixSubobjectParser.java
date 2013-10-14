/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject XROIPPrefixSubobject<IPv4Prefix>}
 */
public class XROIPv4PrefixSubobjectParser {
	public static final int IP_F_LENGTH = 4;
	public static final int PREFIX_F_LENGTH = 1;
	public static final int ATTRIBUTE_LENGTH = 1;

	public static final int IP_F_OFFSET = 0;
	public static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;
	public static final int ATTRIBUTE_OFFSET = PREFIX_F_OFFSET + PREFIX_F_LENGTH;

	public static final int CONTENT_LENGTH = ATTRIBUTE_OFFSET + ATTRIBUTE_LENGTH;

	public static XROIPPrefixSubobject parse(final byte[] soContentsBytes, final boolean mandatory) throws PCEPDeserializerException {
		if (soContentsBytes == null || soContentsBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (soContentsBytes.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		final int length = UnsignedBytes.toInt(soContentsBytes[PREFIX_F_OFFSET]);

		return new XROIPPrefixSubobject(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(soContentsBytes, IP_F_OFFSET, IP_F_LENGTH),
				length)), mandatory, XROSubobjectAttributeMapping.getInstance().getFromAttributeIdentifier(
				(short) (soContentsBytes[ATTRIBUTE_OFFSET] & 0xFF)));
	}

	public static byte[] put(final ExcludeRouteSubobject objToSerialize) {
		if (!(objToSerialize instanceof XROIPPrefixSubobject))
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + objToSerialize.getClass()
					+ ". Needed XROIPPrefixSubobject.");

		final XROIPPrefixSubobject specObj = (XROIPPrefixSubobject) objToSerialize;
		final IpPrefix prefix = specObj.getPrefix();

		if (prefix.getIpv4Prefix() == null)
			throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ". Needed IPv4Prefix.");

		final byte[] retBytes = new byte[CONTENT_LENGTH];
		ByteArray.copyWhole(prefix.getIpv4Prefix().getValue().getBytes(), retBytes, IP_F_OFFSET);
		// sretBytes[PREFIX_F_OFFSET] = (byte) prefix.getIpv4Prefix().;
		retBytes[ATTRIBUTE_OFFSET] = (byte) XROSubobjectAttributeMapping.getInstance().getFromAttributeEnum(specObj.getAttribute());

		return retBytes;
	}
}
