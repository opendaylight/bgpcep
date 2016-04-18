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
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuBgpPrefixSidParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LuBgpPrefixSidParser.class);

    public static final int LABEL_INDEX_TYPE = 1;
    private static final int LABEL_INDEX_VALUE_LENGHT = 4;
    private static final int RESERVED = 1;
    private static final int LABEL_INDEX_FLAGS_BYTES = 2;

    public static final int ORIGINATOR_SRGB_TYPE = 3;
    private static final int ORIGINATOR_FLAGS_BYTES = 2;
    private static final int SRGB_LENGTH = 6;

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final int type, final ByteBuf buffer) {
        switch (type) {
        case LABEL_INDEX_TYPE:
            return parseLabelIndexTlv(buffer);
        case ORIGINATOR_SRGB_TYPE:
            return parseOriginatorSrgbTlv(buffer);
        default:
            LOG.info("Unrecognized LU BGP prefix SID TLV type: {}", type);
            break;
        }
        return null;
    }

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf bytes) {
        if (tlv instanceof LuLabelIndexTlv) {
            serializeLabelIndexTlv((LuLabelIndexTlv) tlv, bytes);
        } else if (tlv instanceof LuOriginatorSrgbTlv) {
            serializeOriginatorTlv((LuOriginatorSrgbTlv) tlv, bytes);
        } else {
            LOG.info("Unsupported LU BGP prefix SID TLV type: {}", tlv.getClass());
        }
    }

    public static void serializeLabelIndexTlv(final LuLabelIndexTlv tlv, final ByteBuf aggregator) {
        aggregator.writeByte(LABEL_INDEX_TYPE);
        aggregator.writeShort(LABEL_INDEX_VALUE_LENGHT + RESERVED + LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeZero(RESERVED);
        aggregator.writeZero(LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeInt(tlv.getLabelIndexTlv().intValue());
    }

    public static void serializeOriginatorTlv(final LuOriginatorSrgbTlv tlv, final ByteBuf aggregator) {
        aggregator.writeByte(ORIGINATOR_SRGB_TYPE);
        aggregator.writeShort(ORIGINATOR_FLAGS_BYTES + SRGB_LENGTH * tlv.getSrgbValue().size());
        aggregator.writeZero(ORIGINATOR_FLAGS_BYTES);
        for (final SrgbValue val : tlv.getSrgbValue()) {
            aggregator.writeMedium(val.getBase().getValue().intValue());
            aggregator.writeMedium(val.getRange().getValue().intValue());
        }
    }

    public static LuLabelIndexTlv parseLabelIndexTlv(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of Label Index tlv exceeds readable bytes of income.");
        buffer.readBytes(RESERVED);
        buffer.readBytes(LABEL_INDEX_FLAGS_BYTES);
        final Long vlaue = buffer.readUnsignedInt();
        return new LuLabelIndexTlv() {
            @Override
            public Long getLabelIndexTlv() {
                return vlaue;
            }
            @Override
            public <E extends Augmentation<LuLabelIndexTlv>> E getAugmentation(final Class<E> augmentationType) {
                return null;
            }
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return LuLabelIndexTlv.class;
            }
        };
    }

    public static LuOriginatorSrgbTlv parseOriginatorSrgbTlv(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of Originator Srgb tlv exceeds readable bytes of income.");
        buffer.readBytes(ORIGINATOR_FLAGS_BYTES);
        final List<SrgbValue> srgbList = parseSrgbs(buffer.readBytes(length - ORIGINATOR_FLAGS_BYTES));
        return new LuOriginatorSrgbTlv() {
            @Override
            public List<SrgbValue> getSrgbValue() {
                return srgbList;
            }
            @Override
            public <E extends Augmentation<LuOriginatorSrgbTlv>> E getAugmentation(final Class<E> augmentationType) {
                return null;
            }
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return LuOriginatorSrgbTlv.class;
            }
        };
    }

    private static List<SrgbValue> parseSrgbs(final ByteBuf buffer) {
        Preconditions.checkState(buffer.readableBytes() % SRGB_LENGTH == 0, "Number of SRGBs doesn't fit available bytes.");
        final List<SrgbValue> ret = new ArrayList<SrgbValue>();
        while (buffer.isReadable()) {
            ret.add(new SrgbValueBuilder().setBase(new Srgb((long) buffer.readUnsignedMedium())).setRange(new Srgb((long) buffer.readUnsignedMedium())).build());
        }
        return ret;
    }
}
