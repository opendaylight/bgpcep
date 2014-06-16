/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.BitSet;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags;

/**
 * Parser for {@link RsvpErrorSpec}
 */
public final class Stateful07RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 21;

    private static final int FLAGS_F_LENGTH = 1;

    private static final int RSVP_ERROR_CLASS_NUM = 6;
    private static final int RSVP_IPV4_ERROR_CLASS_TYPE = 1;
    private static final int RSVP_IPV6_ERROR_CLASS_TYPE = 2;

    private static final int USER_ERROR_CLASS_NUM = 194;
    private static final int USER_ERROR_CLASS_TYPE = 1;

    private static final int IN_PLACE_FLAG_OFFSET = 7;
    private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

    @Override
    public RsvpErrorSpec parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final int classNum = UnsignedBytes.toInt(buffer.readByte());
        final int classType = UnsignedBytes.toInt(buffer.readByte());
        ErrorType errorType = null;
        if (classNum == RSVP_ERROR_CLASS_NUM) {
            errorType = parseRsvp(classType, buffer.slice());
        } else if (classNum == USER_ERROR_CLASS_NUM && classType == USER_ERROR_CLASS_TYPE) {
            errorType = parseUserError(buffer.slice());
        }
        return new RsvpErrorSpecBuilder().setErrorType(errorType).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv != null, "RSVPErrorSpecTlv is mandatory.");
        final RsvpErrorSpec rsvp = (RsvpErrorSpec) tlv;
        final ByteBuf body = Unpooled.buffer();
        if (rsvp.getErrorType().getImplementedInterface().equals(RsvpCase.class)) {
            final RsvpCase r = (RsvpCase) rsvp.getErrorType();
            serializeRsvp(r.getRsvpError(), body);
            TlvUtil.formatTlv(TYPE, body, buffer);
        } else {
            final UserCase u = (UserCase) rsvp.getErrorType();
            serializerUserError(u.getUserError(), body);
            TlvUtil.formatTlv(TYPE, body, buffer);
        }
    }

    private UserCase parseUserError(final ByteBuf buffer) {
        final UserErrorBuilder error = new UserErrorBuilder();
        error.setEnterprise(new EnterpriseNumber(buffer.readUnsignedInt()));
        error.setSubOrg((short) UnsignedBytes.toInt(buffer.readByte()));
        final int errDescrLength = UnsignedBytes.toInt(buffer.readByte());
        error.setValue(buffer.readUnsignedShort());
        error.setDescription(ByteArray.bytesToHRString(ByteArray.readBytes(buffer, errDescrLength)));
        // if we have any subobjects, place the implementation here
        return new UserCaseBuilder().setUserError(error.build()).build();
    }

    private void serializerUserError(final UserError ue, final ByteBuf body) {
        byte[] desc = (ue.getDescription() == null) ? new byte[0] : ue.getDescription().getBytes();
        body.writeByte(USER_ERROR_CLASS_NUM);
        body.writeByte(USER_ERROR_CLASS_TYPE);
        body.writeInt(ue.getEnterprise().getValue().intValue());
        body.writeByte(ue.getSubOrg());
        body.writeByte(desc.length);
        body.writeShort(ue.getValue().shortValue());
        body.writeBytes(desc);
    }

    private RsvpCase parseRsvp(final int classType, final ByteBuf buffer) {
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        if (classType == RSVP_IPV4_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddress(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH))));
        } else if (classType == RSVP_IPV6_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddress(Ipv6Util.addressForBytes(ByteArray.readBytes(buffer, Ipv6Util.IPV6_LENGTH))));
        }
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(buffer, FLAGS_F_LENGTH));
        builder.setFlags(new Flags(flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET)));
        final short errorCode = buffer.readUnsignedByte();
        builder.setCode(errorCode);
        final int errorValue = buffer.readUnsignedShort();
        builder.setValue(errorValue);
        return new RsvpCaseBuilder().setRsvpError(builder.build()).build();
    }

    private void serializeRsvp(final RsvpError rsvp, final ByteBuf body) {
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        flags.set(IN_PLACE_FLAG_OFFSET, rsvp.getFlags().isInPlace());
        flags.set(NOT_GUILTY_FLAGS_OFFSET, rsvp.getFlags().isNotGuilty());
        final IpAddress node = rsvp.getNode();
        if (node.getIpv4Address() != null) {
            body.writeByte(RSVP_ERROR_CLASS_NUM);
            body.writeByte(RSVP_IPV4_ERROR_CLASS_TYPE);
            body.writeBytes(Ipv4Util.bytesForAddress(node.getIpv4Address()));
        } else {
            body.writeByte(RSVP_ERROR_CLASS_NUM);
            body.writeByte(RSVP_IPV6_ERROR_CLASS_TYPE);
            body.writeBytes(Ipv6Util.bytesForAddress(node.getIpv6Address()));
        }
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        body.writeByte(rsvp.getCode());
        body.writeShort(rsvp.getValue().shortValue());
    }
}
