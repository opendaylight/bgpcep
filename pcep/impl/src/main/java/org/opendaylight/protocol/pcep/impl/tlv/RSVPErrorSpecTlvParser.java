/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import java.util.BitSet;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RsvpErrorSpecTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.Rsvp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.RsvpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.UserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.rsvp.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.rsvp.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.error.type.user.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link RsvpErrorSpecTlv}
 */
public class RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

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

	/*
	 * flags offsets inside flags field in bits
	 */
	private static final int IN_PLACE_FLAG_OFFSET = 7;
	private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

	@Override
	public RsvpErrorSpecTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");

		final int classNum = ByteArray.bytesToInt(ByteArray.subByte(valueBytes, 2, 1));
		final int classType = ByteArray.bytesToInt(ByteArray.subByte(valueBytes, 3, 1));

		ErrorType errorType = null;
		int byteOffset = 0;

		if (classNum == RSVP_ERROR_CLASS_NUM) {
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

			final short errorCode = (short) (valueBytes[byteOffset] & 0xFF);
			byteOffset += ERROR_CODE_F_LENGTH;
			final int errorValue = (ByteArray.bytesToShort(ByteArray.subByte(valueBytes, byteOffset, ERROR_VALUE_F_LENGTH)) & 0xFFFF);

			errorType = new RsvpBuilder().setRsvpError(
					builder.setFlags(new Flags(flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET))).setCode(errorCode).setValue(
							errorValue).build()).build();
		} else if (classNum == USER_ERROR_CLASS_NUM && classType == USER_ERROR_CLASS_TYPE) {
			final UserErrorBuilder error = new UserErrorBuilder();
			error.setEnterprise(new EnterpriseNumber(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, byteOffset, ENTERPRISE_F_LENGTH))));
			byteOffset += ENTERPRISE_F_LENGTH;
			error.setSubOrg(ByteArray.bytesToShort(ByteArray.subByte(valueBytes, byteOffset, SUB_ORG_F_LENGTH)));
			byteOffset += SUB_ORG_F_LENGTH;
			final int errDescrLength = UnsignedBytes.toInt(valueBytes[byteOffset]);
			byteOffset += ERR_DESCR_LENGTH_F_LENGTH;
			error.setValue(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, byteOffset, USER_VALUE_F_LENGTH)));
			byteOffset += USER_VALUE_F_LENGTH;
			error.setDescription(ByteArray.bytesToHRString(ByteArray.subByte(valueBytes, byteOffset, errDescrLength)));
			byteOffset += errDescrLength;
			if (byteOffset < valueBytes.length) {
				// TODO: if we have any subobjects
				// error.setSubobjects(new SubobjectsBuilder().build());
			}
			errorType = new UserBuilder().setUserError(error.build()).build();
		}

		return new RsvpErrorSpecBuilder().setErrorType(errorType).build();
	}

	@Override
	public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
		if (tlv == null)
			throw new IllegalArgumentException("RSVPErrorSpecTlv is mandatory.");
		final RsvpErrorSpecTlv rsvp = (RsvpErrorSpecTlv) tlv;

		if (rsvp.getErrorType().getClass().equals(Rsvp.class)) {
			final Rsvp r = (Rsvp) rsvp.getErrorType();
			final RsvpError e = r.getRsvpError();
			final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
			flags.set(IN_PLACE_FLAG_OFFSET, e.getFlags().isInPlace());
			flags.set(NOT_GUILTY_FLAGS_OFFSET, e.getFlags().isNotGuilty());
			final IpAddress node = e.getNode();
			if (node.getIpv4Address() != null) {

			}

		} else {

		}
		// TODO: finish
	}
}
