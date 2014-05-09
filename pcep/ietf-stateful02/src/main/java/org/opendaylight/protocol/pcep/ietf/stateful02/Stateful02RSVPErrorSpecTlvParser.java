/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import java.util.BitSet;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.impl.tlv.TlvUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link RsvpErrorSpec}
 */
public final class Stateful02RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 21;

	private static final int IP4_F_LENGTH = 4;
	private static final int IP6_F_LENGTH = 16;
	private static final int FLAGS_F_LENGTH = 1;
	private static final int ERROR_CODE_F_LENGTH = 1;
	private static final int ERROR_VALUE_F_LENGTH = 2;

	private static final int IN_PLACE_FLAG_OFFSET = 7;
	private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

	private static final int V4_RSVP_LENGTH = 8;
	private static final int V6_RSVP_LENGTH = 20;

	@Override
	public RsvpErrorSpec parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}
		int byteOffset = 0;
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		if (valueBytes.length == V4_RSVP_LENGTH) {
			builder.setNode(new IpAddress(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, byteOffset, IP4_F_LENGTH))));
			byteOffset += IP4_F_LENGTH;
		} else if (valueBytes.length == V6_RSVP_LENGTH) {
			builder.setNode(new IpAddress(Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, byteOffset, IP6_F_LENGTH))));
			byteOffset += IP6_F_LENGTH;
		}
		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(valueBytes, byteOffset, FLAGS_F_LENGTH));
		byteOffset += FLAGS_F_LENGTH;
		builder.setFlags(new Flags(flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET)));
		final short errorCode = (short) UnsignedBytes.toInt(valueBytes[byteOffset]);
		byteOffset += ERROR_CODE_F_LENGTH;
		builder.setCode(errorCode);
		final int errorValue = (ByteArray.bytesToShort(ByteArray.subByte(valueBytes, byteOffset, ERROR_VALUE_F_LENGTH)) & 0xFFFF);
		builder.setValue(errorValue);
		return new RsvpErrorSpecBuilder().setRsvpError(builder.build()).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("RSVPErrorSpecTlv is mandatory.");
		}
		final RsvpErrorSpec rsvpTlv = (RsvpErrorSpec) tlv;
		final RsvpError rsvp = rsvpTlv.getRsvpError();
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(IN_PLACE_FLAG_OFFSET, rsvp.getFlags().isInPlace());
		flags.set(NOT_GUILTY_FLAGS_OFFSET, rsvp.getFlags().isNotGuilty());
		int offset = 0;
		final IpAddress node = rsvp.getNode();
		byte[] bytes;
		if (node.getIpv4Address() != null) {
			bytes = new byte[V4_RSVP_LENGTH];
			ByteArray.copyWhole(Ipv4Util.bytesForAddress(node.getIpv4Address()), bytes, offset);
			offset += IP4_F_LENGTH;
		} else {
			bytes = new byte[V6_RSVP_LENGTH];
			ByteArray.copyWhole(Ipv6Util.bytesForAddress(node.getIpv6Address()), bytes, offset);
			offset += IP6_F_LENGTH;
		}
		bytes[offset] = ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH)[0];
		offset += FLAGS_F_LENGTH;
		bytes[offset] = UnsignedBytes.checkedCast(rsvp.getCode());
		offset += ERROR_CODE_F_LENGTH;
		final byte[] value = ByteArray.intToBytes(rsvp.getValue().intValue(), ERROR_VALUE_F_LENGTH);
		ByteArray.copyWhole(value, bytes, offset);
		return TlvUtil.formatTlv(TYPE, bytes);
	}
}
