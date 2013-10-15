/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link IpPrefixSubobject}
 */
public class EROIpPrefixSubobjectParser implements SubobjectParser, SubobjectSerializer {

	public static final int TYPE = 32;

	public static final int IP4_F_LENGTH = 4;
	public static final int PREFIX4_F_LENGTH = 1;

	public static final int PREFIX4_F_OFFSET = IP4_F_LENGTH;

	public static final int CONTENT4_LENGTH = PREFIX4_F_OFFSET + PREFIX4_F_LENGTH + 1; //added reserved field of size 1 byte

	public static final int IP_F_LENGTH = 16;
	public static final int PREFIX_F_LENGTH = 1;

	public static final int IP_F_OFFSET = 0;
	public static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;

	public static final int CONTENT_LENGTH = PREFIX_F_OFFSET + PREFIX_F_LENGTH + 1; // added reserved field of size 1 byte

	@Override
	public IpPrefixSubobject parseSubobject(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

		if (buffer.length == CONTENT4_LENGTH) {
			final int length = UnsignedBytes.toInt(buffer[PREFIX4_F_OFFSET]);
			return new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(buffer, 0, IP4_F_LENGTH), length))).build();
		} else if (buffer.length == CONTENT_LENGTH) {
			final int length = UnsignedBytes.toInt(buffer[PREFIX_F_OFFSET]);
			return new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(buffer, 0, IP_F_LENGTH), length))).build();
		} else {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + ";");
		}
	}

	@Override
	public byte[] serializeSubobject(final CSubobject subobject) {
		if (!(subobject instanceof IpPrefixSubobject)) {
			throw new IllegalArgumentException("Unknown subobject instance. Passed " + subobject.getClass()
					+ ". Needed IpPrefixSubobject.");
		}

		final IpPrefixSubobject specObj = (IpPrefixSubobject) subobject;
		final IpPrefix prefix = specObj.getIpPrefix();

		if (prefix.getIpv4Prefix() != null) {
			final byte[] retBytes = new byte[CONTENT4_LENGTH];
			ByteArray.copyWhole(prefix.getIpv4Prefix().getValue().getBytes(), retBytes, 0);
			return retBytes;
		} else if (prefix.getIpv6Prefix() != null) {
			final byte[] retBytes = new byte[CONTENT4_LENGTH];
			ByteArray.copyWhole(prefix.getIpv4Prefix().getValue().getBytes(), retBytes, 0);
			return retBytes;
			//retBytes[PREFIX_F_OFFSET] = ByteArray.intToBytes(prefix.getLength())[Integer.SIZE / Byte.SIZE - 1];
		}
		return new byte[0];
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
