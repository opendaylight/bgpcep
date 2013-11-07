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
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link IpPrefixSubobject}
 */
public class XROIpPrefixSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 1;

	public static final int TYPE6 = 2;

	private static final int IP4_F_LENGTH = 4;
	private static final int PREFIX_F_LENGTH = 1;
	private static final int ATTRIBUTE_LENGTH = 1;

	private static final int IP_F_OFFSET = 0;
	private static final int PREFIX4_F_OFFSET = IP_F_OFFSET + IP4_F_LENGTH;
	private static final int ATTRIBUTE4_OFFSET = PREFIX4_F_OFFSET + PREFIX_F_LENGTH;

	private static final int CONTENT4_LENGTH = ATTRIBUTE4_OFFSET + ATTRIBUTE_LENGTH;

	private static final int IP6_F_LENGTH = 16;
	private static final int PREFIX6_F_OFFSET = IP_F_OFFSET + IP6_F_LENGTH;
	private static final int ATTRIBUTE6_OFFSET = PREFIX6_F_OFFSET + PREFIX_F_LENGTH;

	private static final int CONTENT6_LENGTH = ATTRIBUTE6_OFFSET + ATTRIBUTE_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean mandatory) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setMandatory(mandatory);
		if (buffer.length == CONTENT4_LENGTH) {
			final int length = UnsignedBytes.toInt(buffer[PREFIX4_F_OFFSET]);
			builder.setAttribute(Attribute.forValue(UnsignedBytes.toInt(buffer[ATTRIBUTE4_OFFSET])));
			builder.setSubobjectType(new IpPrefixBuilder().setIpPrefix(
					new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(buffer, IP_F_OFFSET, IP4_F_LENGTH), length))).build());
		} else if (buffer.length == CONTENT6_LENGTH) {
			final int length = UnsignedBytes.toInt(buffer[PREFIX6_F_OFFSET]);
			builder.setAttribute(Attribute.forValue(UnsignedBytes.toInt(buffer[ATTRIBUTE6_OFFSET])));
			builder.setSubobjectType(new IpPrefixBuilder().setIpPrefix(
					new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(buffer, IP_F_OFFSET, IP6_F_LENGTH), length))).build());
		} else {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + ";");
		}
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof IpPrefixSubobject)) {
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed IpPrefixSubobject.");
		}

		final IpPrefixSubobject specObj = (IpPrefixSubobject) subobject.getSubobjectType();
		final IpPrefix prefix = specObj.getIpPrefix();

		if (prefix.getIpv4Prefix() == null && prefix.getIpv6Prefix() == null) {
			throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ".");
		}

		if (prefix.getIpv4Prefix() != null) {
			final byte[] retBytes = new byte[CONTENT4_LENGTH];
			ByteArray.copyWhole(Ipv4Util.bytesForPrefix(prefix.getIpv4Prefix()), retBytes, IP_F_OFFSET);
			retBytes[PREFIX4_F_OFFSET] = ByteArray.intToBytes(Ipv4Util.getPrefixLength(prefix))[Integer.SIZE / Byte.SIZE - 1];
			retBytes[ATTRIBUTE4_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());
			return retBytes;
		} else {
			final byte[] retBytes = new byte[CONTENT6_LENGTH];
			ByteArray.copyWhole(Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()), retBytes, IP_F_OFFSET);
			retBytes[PREFIX6_F_OFFSET] = ByteArray.intToBytes(Ipv4Util.getPrefixLength(prefix))[Integer.SIZE / Byte.SIZE - 1];
			retBytes[ATTRIBUTE6_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());
			return retBytes;
		}
	}

	@Override
	public int getType() {
		return TYPE;
	}

	public int getType6() {
		return TYPE6;
	}
}
