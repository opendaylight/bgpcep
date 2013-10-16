/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject RROIPAddressSubobject<IPv4Prefix>}
 */
public class RROIpPrefixSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

	public static final int TYPE = 1;

	public static final int TYPE6 = 2;

	public static final int IP4_F_LENGTH = 4;
	public static final int PREFIX4_F_LENGTH = 1;
	public static final int FLAGS4_F_LENGTH = 1;

	public static final int IP4_F_OFFSET = 0;
	public static final int PREFIX4_F_OFFSET = IP4_F_OFFSET + IP4_F_LENGTH;
	public static final int FLAGS4_F_OFFSET = PREFIX4_F_OFFSET + PREFIX4_F_LENGTH;

	public static final int CONTENT4_LENGTH = FLAGS4_F_OFFSET + FLAGS4_F_LENGTH;

	public static final int IP_F_LENGTH = 16;
	public static final int PREFIX_F_LENGTH = 1;
	public static final int FLAGS_F_LENGTH = 1;

	public static final int IP_F_OFFSET = 0;
	public static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;
	public static final int FLAGS_F_OFFSET = PREFIX_F_OFFSET + PREFIX_F_LENGTH;

	public static final int CONTENT_LENGTH = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

	/*
	 * flags offset in bits
	 */
	public static final int LPA_F_OFFSET = 7;
	public static final int LPIU_F_OFFSET = 6;

	@Override
	public Subobjects parseSubobject(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.length != CONTENT4_LENGTH || buffer.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + ";");

		final int length = UnsignedBytes.toInt(buffer[PREFIX4_F_OFFSET]);

		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(buffer, FLAGS4_F_OFFSET, FLAGS4_F_OFFSET + FLAGS4_F_LENGTH));

		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setProtectionAvailable(flags.get(LPA_F_OFFSET));
		builder.setProtectionInUse(flags.get(LPIU_F_OFFSET));
		builder.setSubobjectType(new IpPrefixBuilder().setIpPrefix(
				new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(buffer, IP4_F_OFFSET, IP4_F_LENGTH), length))).build());

		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject instanceof IpPrefixSubobject))
			throw new IllegalArgumentException("Unknown ReportedRouteSubobject instance. Passed " + subobject.getClass()
					+ ". Needed RROIPAddressSubobject.");

		final IpPrefixSubobject specObj = (IpPrefixSubobject) subobject;
		final IpPrefix prefix = specObj.getIpPrefix();

		if (prefix.getIpv4Prefix() == null && prefix.getIpv6Prefix() == null)
			throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ".");

		final BitSet flags = new BitSet(FLAGS4_F_LENGTH * Byte.SIZE);

		flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
		flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());

		final byte[] retBytes = new byte[CONTENT4_LENGTH];

		if (prefix.getIpv4Prefix() != null) {
			ByteArray.copyWhole(prefix.getIpv4Prefix().getValue().getBytes(), retBytes, IP4_F_OFFSET);
			retBytes[PREFIX4_F_OFFSET] = ByteArray.intToBytes(Ipv4Util.getPrefixLength(prefix))[Integer.SIZE / Byte.SIZE - 1];
			ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS4_F_LENGTH), retBytes, FLAGS4_F_OFFSET);
		} else {
			ByteArray.copyWhole(prefix.getIpv6Prefix().getValue().getBytes(), retBytes, IP_F_OFFSET);
			retBytes[PREFIX_F_OFFSET] = ByteArray.intToBytes(Ipv4Util.getPrefixLength(prefix))[Integer.SIZE / Byte.SIZE - 1];
			ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);
		}
		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}

	public int getType6() {
		return TYPE6;
	}
}
