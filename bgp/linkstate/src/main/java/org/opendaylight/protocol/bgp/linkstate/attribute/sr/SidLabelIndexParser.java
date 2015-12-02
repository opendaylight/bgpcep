/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.Ipv6AddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.LocalLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SidLabelIndexParser {

    private static final Logger LOG = LoggerFactory.getLogger(SidLabelIndexParser.class);
    private static final int LABEL_MASK = 0xfffff;

    private SidLabelIndexParser() {
        throw new UnsupportedOperationException();
    }

    enum Size {
        LABEL(3), SID(4), IPV6_ADD(16);
        private int size;
        private static final Map<Integer, Size> VALUE_MAP;
        static {
            final ImmutableMap.Builder<java.lang.Integer, Size> b = ImmutableMap.builder();
            for (final Size enumItem : Size.values()){
                b.put(enumItem.size, enumItem);
            }
            VALUE_MAP = b.build();
        }
        Size(final int size) {
            this.size = size;
        }
        static Size forValue(final int value) {
            return VALUE_MAP.get(value);
        }
    }

    static final int SID_TYPE = 1161;

    static ByteBuf serializeSidValue(final SidLabelIndex tlv) {
        if (tlv instanceof Ipv6AddressCase) {
            return Ipv6Util.byteBufForAddress(((Ipv6AddressCase) tlv).getIpv6Address());
        } else if (tlv instanceof LocalLabelCase) {
            return Unpooled.copyMedium(((LocalLabelCase) tlv).getLocalLabel().getValue().intValue() & LABEL_MASK);
        } else if (tlv instanceof SidCase) {
            return Unpooled.copyInt(((SidCase) tlv).getSid().intValue());
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
        return parseSidLabelIndex(Size.forValue(length), buffer);
    }

    static SidLabelIndex parseSidLabelIndex(final Size length, final ByteBuf buffer) {
        switch (length) {
        case LABEL:
            return new LocalLabelCaseBuilder().setLocalLabel(new MplsLabel(new Long(buffer.readUnsignedMedium() & LABEL_MASK))).build();
        case SID:
            return new SidCaseBuilder().setSid(buffer.readUnsignedInt()).build();
        case IPV6_ADD:
            return new Ipv6AddressCaseBuilder().setIpv6Address(Ipv6Util.addressForByteBuf(buffer)).build();
        default:
            return null;
        }
    }

    static void setFlags(final SidLabelIndex tlv, final BitArray flags, final int value, final int local) {
        if (tlv instanceof LocalLabelCase) {
            flags.set(value, Boolean.TRUE);
            flags.set(local, Boolean.TRUE);
        } else if (tlv instanceof SidCase) {
            flags.set(value, Boolean.FALSE);
            flags.set(local, Boolean.FALSE);
        } else if (tlv instanceof Ipv6AddressCase) {
            flags.set(value, Boolean.TRUE);
            flags.set(local, Boolean.FALSE);
        }
    }

}
