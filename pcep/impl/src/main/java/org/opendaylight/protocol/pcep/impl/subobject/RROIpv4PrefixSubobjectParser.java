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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link IpPrefixCase}
 */
public class RROIpv4PrefixSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

	public static final int TYPE = 1;

	private static final int IP4_F_LENGTH = 4;

	private static final int PREFIX_F_LENGTH = 1;
	private static final int FLAGS_F_LENGTH = 1;

	private static final int IP_F_OFFSET = 0;

	private static final int PREFIX4_F_OFFSET = IP_F_OFFSET + IP4_F_LENGTH;
	private static final int FLAGS4_F_OFFSET = PREFIX4_F_OFFSET + PREFIX_F_LENGTH;

	private static final int CONTENT4_LENGTH = IP4_F_LENGTH + PREFIX_F_LENGTH + FLAGS_F_LENGTH;

	private static final int LPA_F_OFFSET = 7;
	private static final int LPIU_F_OFFSET = 6;

	@Override
	public Subobjects parseSubobject(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != CONTENT4_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + ";");
		}
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		final int length = UnsignedBytes.toInt(buffer[PREFIX4_F_OFFSET]);
		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(buffer, FLAGS4_F_OFFSET, FLAGS4_F_OFFSET + FLAGS_F_LENGTH));
		builder.setProtectionAvailable(flags.get(LPA_F_OFFSET));
		builder.setProtectionInUse(flags.get(LPIU_F_OFFSET));
		builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
				new IpPrefixBuilder().setIpPrefix(
						new IpPrefix(Ipv4Util.prefixForBytes(ByteArray.subByte(buffer, IP_F_OFFSET, IP4_F_LENGTH), length))).build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof IpPrefixCase)) {
			throw new IllegalArgumentException("Unknown ReportedRouteSubobject instance. Passed " + subobject.getClass()
					+ ". Needed IpPrefixCase.");
		}
		final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
		final IpPrefix prefix = specObj.getIpPrefix();

		if (prefix.getIpv4Prefix() == null && prefix.getIpv6Prefix() == null) {
			throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ".");
		}
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
		flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());

		if (prefix.getIpv6Prefix() != null) {
			return new RROIpv6PrefixSubobjectParser().serializeSubobject(subobject);
		}
		final byte[] retBytes = new byte[CONTENT4_LENGTH];
		ByteArray.copyWhole(Ipv4Util.bytesForPrefix(prefix.getIpv4Prefix()), retBytes, IP_F_OFFSET);
		retBytes[PREFIX4_F_OFFSET] = UnsignedBytes.checkedCast(Ipv4Util.getPrefixLength(prefix));
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS4_F_OFFSET);
		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
