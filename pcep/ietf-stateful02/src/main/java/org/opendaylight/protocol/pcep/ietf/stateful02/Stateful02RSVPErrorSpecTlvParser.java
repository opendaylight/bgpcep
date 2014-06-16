/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.RsvpError;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.rsvp.error.spec.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ErrorSpec.Flags;

/**
 * Parser for {@link RsvpErrorSpec}
 */
public final class Stateful02RSVPErrorSpecTlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 21;

    private static final int FLAGS_F_LENGTH = 1;

    private static final int IN_PLACE_FLAG_OFFSET = 7;
    private static final int NOT_GUILTY_FLAGS_OFFSET = 6;

    private static final int V4_RSVP_LENGTH = 8;
    private static final int V6_RSVP_LENGTH = 20;

    @Override
    public RsvpErrorSpec parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final RsvpErrorBuilder builder = new RsvpErrorBuilder();
        if (buffer.readableBytes() == V4_RSVP_LENGTH) {
            builder.setNode(new IpAddress(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH))));
        } else if (buffer.readableBytes() == V6_RSVP_LENGTH) {
            builder.setNode(new IpAddress(Ipv6Util.addressForBytes(ByteArray.readBytes(buffer, Ipv6Util.IPV6_LENGTH))));
        }
        final BitSet flags = ByteArray.bytesToBitSet(ByteArray.readBytes(buffer, FLAGS_F_LENGTH));
        builder.setFlags(new Flags(flags.get(IN_PLACE_FLAG_OFFSET), flags.get(NOT_GUILTY_FLAGS_OFFSET)));
        final short errorCode = (short) UnsignedBytes.toInt(buffer.readByte());
        builder.setCode(errorCode);
        final int errorValue = buffer.readUnsignedShort();
        builder.setValue(errorValue);
        return new RsvpErrorSpecBuilder().setRsvpError(builder.build()).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv != null, "RSVPErrorSpecTlv is mandatory.");
        final RsvpErrorSpec rsvpTlv = (RsvpErrorSpec) tlv;
        final RsvpError rsvp = rsvpTlv.getRsvpError();
        final ByteBuf body = Unpooled.buffer();
        final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
        Flags f = rsvp.getFlags();
        if (f.isInPlace() != null) {
            flags.set(IN_PLACE_FLAG_OFFSET, f.isInPlace());
        }
        if (f.isNotGuilty() != null) {
            flags.set(NOT_GUILTY_FLAGS_OFFSET, f.isNotGuilty());
        }
        final IpAddress node = rsvp.getNode();
        if (node.getIpv4Address() != null) {
            body.writeBytes(Ipv4Util.bytesForAddress(node.getIpv4Address()));
        } else {
            body.writeBytes(Ipv6Util.bytesForAddress(node.getIpv6Address()));
        }
        body.writeBytes(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH));
        body.writeByte(rsvp.getCode());
        body.writeShort(rsvp.getValue().shortValue());
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
