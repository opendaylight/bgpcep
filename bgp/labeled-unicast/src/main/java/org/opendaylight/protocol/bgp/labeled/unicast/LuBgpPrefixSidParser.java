/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.OriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;


public final class LuBgpPrefixSidParser {

    private static final int LABEL_INDEX_TYPE = 1;
    private static final int LABEL_INDEX_VALUE_LENGHT = 4;
    private static final int RESERVED = 1;
    private static final int LABEL_INDEX_FLAGS_BYTES = 2;

    private static final int ORIGINATOR_SRGB_TYPE = 3;
    private static final int ORIGINATOR_FLAGS_BYTES = 2;
    private static final int SRGB_LENGTH = 6;

    public static void serializeLabelIndexTlv(final LabelIndexTlv tlv, final ByteBuf aggregator) {
        aggregator.writeByte(LABEL_INDEX_TYPE);
        aggregator.writeShort(LABEL_INDEX_VALUE_LENGHT + RESERVED + LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeZero(RESERVED);
        aggregator.writeZero(LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeInt(tlv.getLabelIndexTlv().intValue());
    }

    public static void serializeOriginatorTlv(final OriginatorSrgbTlv tlv, final ByteBuf aggregator) {
        aggregator.writeByte(ORIGINATOR_SRGB_TYPE);
        aggregator.writeShort(ORIGINATOR_FLAGS_BYTES + SRGB_LENGTH * tlv.getSrgbValue().size());
        aggregator.writeZero(ORIGINATOR_FLAGS_BYTES);
        for (final SrgbValue val : tlv.getSrgbValue()) {
            aggregator.writeMedium(val.getBase().getValue().intValue());
            aggregator.writeMedium(val.getRange().getValue().intValue());
        }
    }

    public static LabelIndexTlv parseLabelIndexTlv(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of Label Index tlv exceeds readable bytes of income.");
        buffer.readBytes(RESERVED);
        buffer.readBytes(LABEL_INDEX_FLAGS_BYTES);
        final Long vlaue = buffer.readUnsignedInt();
        return new LabelIndexTlv() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return LabelIndexTlv.class;
            }
            @Override
            public Long getLabelIndexTlv() {
                return vlaue;
            }
        };
    }

    public static OriginatorSrgbTlv parseOriginatorSrgbTlv(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of Originator Srgb tlv exceeds readable bytes of income.");
        buffer.readBytes(ORIGINATOR_FLAGS_BYTES);
        final List<SrgbValue> srgbList = parseSrgbs(buffer.readBytes(length - ORIGINATOR_FLAGS_BYTES));
        return new OriginatorSrgbTlv() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return OriginatorSrgbTlv.class;
            }
            @Override
            public List<SrgbValue> getSrgbValue() {
                return srgbList;
            }
        };
    }

    private static List<SrgbValue> parseSrgbs(final ByteBuf buffer) {
        Preconditions.checkState(buffer.readableBytes() % SRGB_LENGTH == 0, "Number of SRGBs doesn't fit available bytes.");
        final List<SrgbValue> ret = new ArrayList<SrgbValue>();
        while (buffer.isReadable()) {
            ret.add(new SrgbValueBuilder()
            .setBase(new Srgb((long) buffer.readUnsignedMedium()))
            .setRange(new Srgb((long) buffer.readUnsignedMedium())).build());
        }
        return ret;
    }

}
