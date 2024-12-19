/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.SidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SidLabelIndexParser {
    private static final Logger LOG = LoggerFactory.getLogger(SidLabelIndexParser.class);
    private static final int LABEL_MASK = 0xfffff;
    public static final int SID_LABEL = 1161;

    private SidLabelIndexParser() {
        // Hidden on purpose
    }

    public static ByteBuf serializeSidValue(final SidLabelIndex tlv) {
        return switch (tlv) {
            case LabelCase lc -> Unpooled.copyMedium(lc.getLabel().getValue().intValue() & LABEL_MASK);
            case SidCase sc -> Unpooled.copyInt(sc.getSid().intValue());
            case null, default -> null;
        };
    }

    static SidLabelIndex parseSidSubTlv(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        if (type != SID_LABEL) {
            LOG.warn("Unexpected type in SID/index/label field, expected {}, actual {}, ignoring it", SID_LABEL, type);
            return null;
        }
        return switch (buffer.readUnsignedShort()) {
            case 3 -> new LabelCaseBuilder().setLabel(new MplsLabel(readLabel(buffer))).build();
            case 4 -> new SidCaseBuilder().setSid(readUint32(buffer)).build();
            default -> null;
        };
    }

    private static Uint32 readLabel(final ByteBuf buffer) {
        return Uint32.valueOf(buffer.readUnsignedMedium() & LABEL_MASK);
    }

    /**
     * Parses SID/Label/Index value into appropriate type based on V-Flag and L-Flag values. This method is required as
     * some device-side implementations incorrectly encode SID/Label/Index value using wrong type e.g. Label type to
     * encode an Index value (V-Flag=false, L-Flag=false).
     *
     * @param buffer buffer containing SID/Label/Index value
     * @param isValue V-Flag value
     * @param isLocal L-Flag value
     * @return SID/Label/Index value parsed into the appropriate type
     */
    public static SidLabelIndex parseSidLabelIndexByFlags(final ByteBuf buffer, final boolean isValue,
            final boolean isLocal) {
        final Uint32 sidLabelIndex;
        switch (buffer.readableBytes()) {
            case 3 -> sidLabelIndex = readLabel(buffer);
            case 4 -> sidLabelIndex = readUint32(buffer);
            default -> {
                return null;
            }
        }

        if (isValue != isLocal) {
            return null;
        }
        return isValue ? new LabelCaseBuilder().setLabel(new MplsLabel(sidLabelIndex)).build()
            : new SidCaseBuilder().setSid(sidLabelIndex).build();
    }

    static void setFlags(final SidLabelIndex tlv, final BitArray flags, final int value, final int local) {
        final Boolean flag;
        switch (tlv) {
            case LabelCase lc -> flag = Boolean.TRUE;
            case SidCase sc -> flag = Boolean.FALSE;
            case null, default -> {
                // no-op
                return;
            }
        }
        flags.set(value, flag);
        flags.set(local, flag);
    }
}
