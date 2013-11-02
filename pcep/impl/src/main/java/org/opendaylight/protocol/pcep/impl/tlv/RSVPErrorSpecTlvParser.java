/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.BitSet;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.Rsvp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.User;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user.UserError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link RsvpErrorSpec}
 */
public final class RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 21;

	private static final int IP4_F_LENGTH = 4;
	private static final int IP6_F_LENGTH = 16;
	private static final int FLAGS_F_LENGTH = 1;
	private static final int ERROR_CODE_F_LENGTH = 1;
	private static final int ERROR_VALUE_F_LENGTH = 2;

	private static final int ENTERPRISE_F_LENGTH = 4;
	private static final int SUB_ORG_F_LENGTH = 1;
	private static final int ERR_DESCR_LENGTH_F_LENGTH = 1;
	private static final int USER_VALUE_F_LENGTH = 2;

	private static final int RSVP_ERROR_CLASS_NUM = 6;
	private static final int RSVP_IPV4_ERROR_CLASS_TYPE = 1;
	private static final int RSVP_IPV6_ERROR_CLASS_TYPE = 2;

	private static final int USER_ERROR_CLASS_NUM = 194;
	private static final int USER_ERROR_CLASS_TYPE = 1;

	private static final int IN_PLACE_FLAG_OFFSET = 7;
	private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

	private static final int V4_RSVP_LENGTH = 10;
	private static final int V6_RSVP_LENGTH = 22;

	@Override
	public RsvpErrorSpec parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}

		final int classNum = ByteArray.bytesToInt(ByteArray.subByte(valueBytes, 0, 1));
		final int classType = ByteArray.bytesToInt(ByteArray.subByte(valueBytes, 1, 1));

		ErrorType errorType = null;
		final int byteOffset = 2;

		if (classNum == RSVP_ERROR_CLASS_NUM) {
			errorType = parseRsvp(classType, ByteArray.cutBytes(valueBytes, byteOffset));
		} else if (classNum == USER_ERROR_CLASS_NUM && classType == USER_ERROR_CLASS_TYPE) {
			errorType = parseUserError(ByteArray.cutBytes(valueBytes, byteOffset));
		}
		return new RsvpErrorSpecBuilder().setErrorType(errorType).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("RSVPErrorSpecTlv is mandatory.");
		}
		final RsvpErrorSpec rsvp = (RsvpErrorSpec) tlv;

		if (rsvp.getErrorType().getImplementedInterface().equals(Rsvp.class)) {
			final Rsvp r = (Rsvp) rsvp.getErrorType();
			return serializeRsvp(r.getRsvpError());
		} else {
			final User u = (User) rsvp.getErrorType();
			return serializerUserError(u.getUserError());
		}
	}

	private User parseUserError(final byte[] valueBytes) {
		final UserErrorBuilder error = new UserErrorBuilder();
		int byteOffset = 0;
		error.setEnterprise(new EnterpriseNumber(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, byteOffset, ENTERPRISE_F_LENGTH))));
		byteOffset += ENTERPRISE_F_LENGTH;
		error.setSubOrg((short) UnsignedBytes.toInt(valueBytes[byteOffset]));
		byteOffset += SUB_ORG_F_LENGTH;
		final int errDescrLength = UnsignedBytes.toInt(valueBytes[byteOffset]);
		byteOffset += ERR_DESCR_LENGTH_F_LENGTH;
		error.setValue(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, byteOffset, USER_VALUE_F_LENGTH)));
		byteOffset += USER_VALUE_F_LENGTH;
		error.setDescription(ByteArray.bytesToHRString(ByteArray.subByte(valueBytes, byteOffset, errDescrLength)));
		byteOffset += errDescrLength;
		// TODO: if we have any subobjects
		return new UserBuilder().setUserError(error.build()).build();
	}

	private byte[] serializerUserError(final UserError ue) {
		final byte[] enterprise = ByteArray.subByte(ByteArray.longToBytes(ue.getEnterprise().getValue()), 4, ENTERPRISE_F_LENGTH);
		final byte suborg = UnsignedBytes.checkedCast(ue.getSubOrg());
		final byte[] value = ByteArray.subByte(ByteArray.intToBytes(ue.getValue()), 2, USER_VALUE_F_LENGTH);
		final byte[] desc = (ue.getDescription() == null) ? new byte[0] : ue.getDescription().getBytes();
		final byte descLen = UnsignedBytes.checkedCast(desc.length);
		// TODO: if we have any subobjects
		final byte[] bytes = new byte[2 + ENTERPRISE_F_LENGTH + SUB_ORG_F_LENGTH + USER_VALUE_F_LENGTH + ERR_DESCR_LENGTH_F_LENGTH
				+ desc.length];
		bytes[0] = UnsignedBytes.checkedCast(USER_ERROR_CLASS_NUM);
		bytes[1] = UnsignedBytes.checkedCast(USER_ERROR_CLASS_TYPE);
		int offset = 2;
		ByteArray.copyWhole(enterprise, bytes, offset);
		offset += ENTERPRISE_F_LENGTH;
		bytes[offset] = suborg;
		offset += SUB_ORG_F_LENGTH;
		bytes[offset] = descLen;
		offset += ERR_DESCR_LENGTH_F_LENGTH;
		ByteArray.copyWhole(value, bytes, offset);
		offset += USER_VALUE_F_LENGTH;
		ByteArray.copyWhole(desc, bytes, offset);
		return bytes;
	}

	private Rsvp parseRsvp(final int classType, final byte[] valueBytes) {
		int byteOffset = 0;
		final RsvpErrorBuilder builder = new RsvpErrorBuilder();
		if (classType == RSVP_IPV4_ERROR_CLASS_TYPE) {
			builder.setNode(new IpAddress(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, byteOffset, IP4_F_LENGTH))));
			byteOffset += IP4_F_LENGTH;
		} else if (classType == RSVP_IPV6_ERROR_CLASS_TYPE) {
			builder.setNode(new IpAddress(Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, byteOffset, IP6_F_LENGTH))));
			byteOffset += IP6_F_LENGTH;
		}
		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(valueBytes, byteOffset, FLAGS_F_LENGTH));
		byteOffset += FLAGS_F_LENGTH;
		builder.setFlags(new Flags(flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET)));
		final short errorCode = (short) (valueBytes[byteOffset] & Util.BYTE_MAX_VALUE_BYTES);
		byteOffset += ERROR_CODE_F_LENGTH;
		builder.setCode(errorCode);
		final int errorValue = (ByteArray.bytesToShort(ByteArray.subByte(valueBytes, byteOffset, ERROR_VALUE_F_LENGTH)) & 0xFFFF);
		builder.setValue(errorValue);
		return new RsvpBuilder().setRsvpError(builder.build()).build();
	}

	private byte[] serializeRsvp(final RsvpError rsvp) {
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(IN_PLACE_FLAG_OFFSET, rsvp.getFlags().isInPlace());
		flags.set(NOT_GUILTY_FLAGS_OFFSET, rsvp.getFlags().isNotGuilty());
		int offset = 0;
		final IpAddress node = rsvp.getNode();
		byte[] bytes;
		if (node.getIpv4Address() != null) {
			bytes = new byte[V4_RSVP_LENGTH];
			bytes[0] = RSVP_ERROR_CLASS_NUM;
			bytes[1] = RSVP_IPV4_ERROR_CLASS_TYPE;
			offset += 2;
			ByteArray.copyWhole(Ipv4Util.bytesForAddress(node.getIpv4Address()), bytes, offset);
			offset += IP4_F_LENGTH;
		} else {
			bytes = new byte[V6_RSVP_LENGTH];
			bytes[0] = RSVP_ERROR_CLASS_NUM;
			bytes[1] = RSVP_IPV6_ERROR_CLASS_TYPE;
			offset += 2;
			ByteArray.copyWhole(Ipv6Util.bytesForAddress(node.getIpv6Address()), bytes, offset);
			offset += IP6_F_LENGTH;
		}
		bytes[offset] = ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH)[0];
		offset += FLAGS_F_LENGTH;
		bytes[offset] = UnsignedBytes.checkedCast(rsvp.getCode());
		offset += ERROR_CODE_F_LENGTH;
		final byte[] value = ByteArray.subByte(ByteArray.intToBytes(rsvp.getValue().intValue()), 2, ERROR_VALUE_F_LENGTH);
		ByteArray.copyWhole(value, bytes, offset);
		return bytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
