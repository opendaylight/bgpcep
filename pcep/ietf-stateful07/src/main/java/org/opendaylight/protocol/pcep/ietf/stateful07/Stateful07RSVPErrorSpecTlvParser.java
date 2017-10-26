/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ErrorSpec.Flags;

/**
 * Parser for {@link RsvpErrorSpec}
 */
public final class Stateful07RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 21;

    private static final int FLAGS_SIZE = 8;
    private static final int HEADER_LENGTH = 4;

    private static final int RSVP_ERROR_CLASS_NUM = 6;
    private static final int RSVP_IPV4_ERROR_CLASS_TYPE = 1;
    private static final int RSVP_IPV6_ERROR_CLASS_TYPE = 2;

    private static final int USER_ERROR_CLASS_NUM = 194;
    private static final int USER_ERROR_CLASS_TYPE = 1;

    private static final int IN_PLACE = 7;
    private static final int NOT_GUILTY = 6;

    @Override
    public RsvpErrorSpec parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        // throw away contents of length field
        buffer.readUnsignedShort();
        final int classNum = buffer.readUnsignedByte();
        final int classType = buffer.readUnsignedByte();
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
        Preconditions.checkArgument(tlv instanceof RsvpErrorSpec, "RSVPErrorSpecTlv is mandatory.");
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

    private static UserCase parseUserError(final ByteBuf buffer) {
        final UserErrorBuilder error = new UserErrorBuilder();
        error.setEnterprise(new EnterpriseNumber(buffer.readUnsignedInt()));
        error.setSubOrg(buffer.readUnsignedByte());
        final int errDescrLength = buffer.readUnsignedByte();
        error.setValue(buffer.readUnsignedShort());
        error.setDescription(ByteArray.bytesToHRString(ByteArray.readBytes(buffer, errDescrLength)));
        // if we have any subobjects, place the implementation here
        return new UserCaseBuilder().setUserError(error.build()).build();
    }

    private static void serializerUserError(final UserError ue, final ByteBuf body) {
        final byte[] desc = ue.getDescription() == null ? new byte[0] : ue.getDescription().getBytes(StandardCharsets.UTF_8);
        final ByteBuf userErrorBuf = Unpooled.buffer();
        Preconditions.checkArgument(ue.getEnterprise() != null, "EnterpriseNumber is mandatory");
        writeUnsignedInt(ue.getEnterprise().getValue(), userErrorBuf);
        writeUnsignedByte(ue.getSubOrg(), userErrorBuf);
        userErrorBuf.writeByte(desc.length);
        Preconditions.checkArgument(ue.getValue() != null, "Value is mandatory.");
        writeUnsignedShort(ue.getValue(), userErrorBuf);
        userErrorBuf.writeBytes(desc);
        userErrorBuf.writeZero(TlvUtil.getPadding(desc.length, TlvUtil.PADDED_TO));
        formatRSVPObject(USER_ERROR_CLASS_NUM, USER_ERROR_CLASS_TYPE, userErrorBuf, body);
    }

    private static RsvpCase parseRsvp(final int classType, final ByteBuf buffer) {
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        if (classType == RSVP_IPV4_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddress(Ipv4Util.addressForByteBuf(buffer)));
        } else if (classType == RSVP_IPV6_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddress(Ipv6Util.addressForByteBuf(buffer)));
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setFlags(new Flags(flags.get(IN_PLACE), flags.get(NOT_GUILTY)));
        final short errorCode = buffer.readUnsignedByte();
        builder.setCode(errorCode);
        final int errorValue = buffer.readUnsignedShort();
        builder.setValue(errorValue);
        return new RsvpCaseBuilder().setRsvpError(builder.build()).build();
    }

    private static void serializeRsvp(final RsvpError rsvp, final ByteBuf body) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(IN_PLACE, rsvp.getFlags().isInPlace());
        flags.set(NOT_GUILTY, rsvp.getFlags().isNotGuilty());
        final IpAddress node = rsvp.getNode();
        Preconditions.checkArgument(node != null, "Node is mandatory.");
        final ByteBuf rsvpObjBuf = Unpooled.buffer();
        int type = 0;
        if (node.getIpv4Address() != null) {
            type = RSVP_IPV4_ERROR_CLASS_TYPE;
            writeIpv4Address(node.getIpv4Address(), rsvpObjBuf);
        } else {
            type = RSVP_IPV6_ERROR_CLASS_TYPE;
            writeIpv6Address(node.getIpv6Address(), rsvpObjBuf);
        }
        flags.toByteBuf(rsvpObjBuf);
        Preconditions.checkArgument(rsvp.getCode() != null, "Code is mandatory.");
        writeUnsignedByte(rsvp.getCode(), rsvpObjBuf);
        Preconditions.checkArgument(rsvp.getValue() != null, "Value is mandatory.");
        writeUnsignedShort(rsvp.getValue(), rsvpObjBuf);
        formatRSVPObject(RSVP_ERROR_CLASS_NUM, type, rsvpObjBuf, body);
    }

    private static void formatRSVPObject(final int objClass, final int type, final ByteBuf body, final ByteBuf out) {
        out.writeShort(body.writerIndex() + HEADER_LENGTH);
        out.writeByte(objClass);
        out.writeByte(type);
        out.writeBytes(body);
    }
}
