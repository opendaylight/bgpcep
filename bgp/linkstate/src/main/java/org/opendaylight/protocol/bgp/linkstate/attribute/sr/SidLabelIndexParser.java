/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SidLabelIndexParser {

    private static final Logger LOG = LoggerFactory.getLogger(SidLabelIndexParser.class);

    private SidLabelIndexParser() {
        throw new UnsupportedOperationException();
    }

    static final int SID_TYPE = 1161;
    private static final int LABEL_SIZE = 3;
    private static final int SID_SIZE = 4;
    private static final int IPV6_ADD_SIZE = 16;
    private static final int MASK_20BITS = 1048575;

    static ByteBuf serializeSidValue(final SidLabelIndex tlv) {
        if (tlv instanceof Ipv6AddressCase) {
            return Ipv6Util.byteBufForAddress(((Ipv6AddressCase) tlv).getIpv6Address());
        } else if (tlv instanceof LocalLabelCase) {
            return Unpooled.EMPTY_BUFFER.writeMedium(((LocalLabelCase) tlv).getLocalLabel().intValue() & MASK_20BITS);
        } else if (tlv instanceof SidCase) {
            return Unpooled.EMPTY_BUFFER.writeInt(((SidCase) tlv).getSid().intValue());
        }
        return null;
    }

    static SidLabelIndex parseSidSubTlv(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        if (type != SID_TYPE) {
            LOG.warn("Unexpected type in SID/index/label field, expected {}, actual {}, ignoring it", SID_TYPE, type);
            return null;
        }
        final int length = buffer.readUnsignedShort();
        return parseSidLabelIndex(length, buffer);
    }

    static SidLabelIndex parseSidLabelIndex(final int length, final ByteBuf buffer) {
        if (length == LABEL_SIZE) {
            return new LocalLabelCaseBuilder().setLocalLabel((long) buffer.readUnsignedMedium()).build();
        } else if (length == SID_SIZE) {
            return new SidCaseBuilder().setSid(buffer.readUnsignedInt()).build();
        } else if (length == IPV6_ADD_SIZE) {
            return new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(buffer)).build();
        }
        return null;
    }

    static int getLength(final BitArray flags, final int value, final int local) {
        if (flags.get(value) && flags.get(local)) {
            return LABEL_SIZE;
        }
        if (!flags.get(value) && !flags.get(local)) {
            return SID_SIZE;
        }
        return 0;
    }

    static void setFlags(final SidLabelIndex tlv, final BitArray flags, final int value, final int local) {
        if (tlv instanceof LocalLabelCase) {
            flags.set(value, Boolean.TRUE);
            flags.set(local, Boolean.TRUE);
        } if (tlv instanceof SidCase) {
            flags.set(value, Boolean.FALSE);
            flags.set(local, Boolean.FALSE);
        } if (tlv instanceof Ipv6AddressCase) {
            flags.set(value, Boolean.TRUE);
            flags.set(local, Boolean.FALSE);
        }
    }

}
