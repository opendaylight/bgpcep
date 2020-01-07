/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static com.google.common.base.Preconditions.checkArgument;

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ErrorSpec.Flags;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link RsvpErrorSpec}.
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
        checkArgument(tlv instanceof RsvpErrorSpec, "RSVPErrorSpecTlv is mandatory.");
        final RsvpErrorSpec rsvp = (RsvpErrorSpec) tlv;
        final ByteBuf body = Unpooled.buffer();
        if (rsvp.getErrorType().implementedInterface().equals(RsvpCase.class)) {
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
        final UserErrorBuilder error = new UserErrorBuilder()
                .setEnterprise(new EnterpriseNumber(ByteBufUtils.readUint32(buffer)));
        error.setSubOrg(ByteBufUtils.readUint8(buffer));
        final int errDescrLength = buffer.readUnsignedByte();
        error.setValue(ByteBufUtils.readUint16(buffer));
        error.setDescription(ByteArray.bytesToHRString(ByteArray.readBytes(buffer, errDescrLength)));
        // if we have any subobjects, place the implementation here
        return new UserCaseBuilder().setUserError(error.build()).build();
    }

    private static void serializerUserError(final UserError ue, final ByteBuf body) {
        final String description = ue.getDescription();
        final byte[] desc = description == null ? new byte[0] : description.getBytes(StandardCharsets.UTF_8);
        final ByteBuf userErrorBuf = Unpooled.buffer();
        final EnterpriseNumber enterprise = ue.getEnterprise();
        checkArgument(enterprise != null, "EnterpriseNumber is mandatory");
        ByteBufUtils.write(userErrorBuf, enterprise.getValue());
        ByteBufUtils.writeOrZero(userErrorBuf, ue.getSubOrg());
        userErrorBuf.writeByte(desc.length);
        ByteBufUtils.writeMandatory(userErrorBuf, ue.getValue(), "Value");
        userErrorBuf.writeBytes(desc);
        userErrorBuf.writeZero(TlvUtil.getPadding(desc.length, TlvUtil.PADDED_TO));
        formatRSVPObject(USER_ERROR_CLASS_NUM, USER_ERROR_CLASS_TYPE, userErrorBuf, body);
    }

    private static RsvpCase parseRsvp(final int classType, final ByteBuf buffer) {
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        if (classType == RSVP_IPV4_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer)));
        } else if (classType == RSVP_IPV6_ERROR_CLASS_TYPE) {
            builder.setNode(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer)));
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setFlags(new Flags(flags.get(IN_PLACE), flags.get(NOT_GUILTY)));
        builder.setCode(ByteBufUtils.readUint8(buffer));
        builder.setValue(ByteBufUtils.readUint16(buffer));
        return new RsvpCaseBuilder().setRsvpError(builder.build()).build();
    }

    private static void serializeRsvp(final RsvpError rsvp, final ByteBuf body) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(IN_PLACE, rsvp.getFlags().isInPlace());
        flags.set(NOT_GUILTY, rsvp.getFlags().isNotGuilty());
        final IpAddressNoZone node = rsvp.getNode();
        checkArgument(node != null, "Node is mandatory.");
        final ByteBuf rsvpObjBuf = Unpooled.buffer();
        int type = 0;
        if (node.getIpv4AddressNoZone() != null) {
            type = RSVP_IPV4_ERROR_CLASS_TYPE;
            Ipv4Util.writeIpv4Address(node.getIpv4AddressNoZone(), rsvpObjBuf);
        } else {
            type = RSVP_IPV6_ERROR_CLASS_TYPE;
            Ipv6Util.writeIpv6Address(node.getIpv6AddressNoZone(), rsvpObjBuf);
        }
        flags.toByteBuf(rsvpObjBuf);
        ByteBufUtils.writeMandatory(rsvpObjBuf, rsvp.getCode(), "Code");
        ByteBufUtils.writeMandatory(rsvpObjBuf, rsvp.getValue(), "Value");
        formatRSVPObject(RSVP_ERROR_CLASS_NUM, type, rsvpObjBuf, body);
    }

    private static void formatRSVPObject(final int objClass, final int type, final ByteBuf body, final ByteBuf out) {
        out.writeShort(body.writerIndex() + HEADER_LENGTH);
        out.writeByte(objClass);
        out.writeByte(type);
        out.writeBytes(body);
    }
}
