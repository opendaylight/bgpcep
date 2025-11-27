/*
 * Copyright (c) 2025 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.adjustment.interval.tlv.AdjustmentIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.adjustment.threshold.percentage.tlv.AdjustmentThresholdPercentageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.adjustment.threshold.tlv.AdjustmentThresholdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.auto.bandwidth.attributes.tlv.AutoBandwidthAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.auto.bandwidth.attributes.tlv.AutoBandwidthAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.down.adjustment.interval.tlv.DownAdjustmentIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.down.adjustment.threshold.percentage.tlv.DownAdjustmentThresholdPercentageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.down.adjustment.threshold.tlv.DownAdjustmentThresholdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.maximum.bandwitdh.tlv.MaximumBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.minimum.bandwitdh.tlv.MinimumBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.overflow.threshold.percentage.tlv.OverflowThresholdPercentageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.overflow.threshold.tlv.OverflowThresholdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sample.interval.tlv.SampleIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.underflow.threshold.percentage.tlv.UnderflowThresholdPercentageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.underflow.threshold.tlv.UnderflowThresholdBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class AutoBandwidthAttributesTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 37;

    // Sub-TLVs registry
    private static final int SAMPLE_INTERVAL = 1;
    private static final int ADJUSTMENT_INTERVAL = 2;
    private static final int DOWN_ADJ_INTERVAL = 3;
    private static final int ADJUSTMENT_THRESHOLD = 4;
    private static final int ADJ_THRESHOLD_PERCENTAGE = 5;
    private static final int DOWN_ADJ_THRESHOLD = 6;
    private static final int DOWN_ADJ_THRESHOLD_PERCENTAGE = 7;
    private static final int MINIMUN_BANDWIDTH = 8;
    private static final int MAXIMUM_BANDWIDTH = 9;
    private static final int OVERFLOW_THRESHOLD = 10;
    private static final int OVERFLOW_THRESHOLD_PERCENTAGE = 11;
    private static final int UNDERFLOW_THRESHOLD = 12;
    private static final int UNDERFLOW_THRESHOLD_PERCENTAGE = 13;

    private static final int CONTENT_LENGTH = 4;
    private static final int DOUBLE_CONTENT_LENGTH = 8;
    private static final int RESERVED_2 = 2;
    private static final int RESERVED_3 = 3;
    private static final int MAXIMUM_LENGTH = TlvUtil.HEADER_SIZE * 13 + CONTENT_LENGTH * 7 + DOUBLE_CONTENT_LENGTH * 6;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof AutoBandwidthAttributes, "Auto-Bandwidth Attributes is mandatory.");
        final ByteBuf body = Unpooled.buffer(MAXIMUM_LENGTH);
        serializeSubTlvs((AutoBandwidthAttributes) tlv, body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    private static void serializeSubTlvs(final AutoBandwidthAttributes aba, final ByteBuf body) {
        if (aba.getSampleInterval() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeUint32(subBody, aba.getSampleInterval().getInterval());
            TlvUtil.formatTlv(SAMPLE_INTERVAL, subBody, body);
        }
        if (aba.getAdjustmentInterval() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeUint32(subBody, aba.getAdjustmentInterval().getAdjustment());
            TlvUtil.formatTlv(ADJUSTMENT_INTERVAL, subBody, body);
        }
        if (aba.getDownAdjustmentInterval() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeUint32(subBody, aba.getDownAdjustmentInterval().getDownAdjustment());
            TlvUtil.formatTlv(DOWN_ADJ_INTERVAL, subBody, body);
        }
        if (aba.getAdjustmentThreshold() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeFloat32(aba.getAdjustmentThreshold().getBandwidth(), subBody);
            TlvUtil.formatTlv(ADJUSTMENT_THRESHOLD, subBody, body);
        }
        if (aba.getAdjustmentThresholdPercentage() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            subBody.writeZero(RESERVED_3);
            writeUint8(subBody, aba.getAdjustmentThresholdPercentage().getPercentage());
            writeFloat32(aba.getAdjustmentThresholdPercentage().getBandwidth(), subBody);
            TlvUtil.formatTlv(ADJ_THRESHOLD_PERCENTAGE, subBody, body);
        }
        if (aba.getDownAdjustmentThreshold() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeFloat32(aba.getDownAdjustmentThreshold().getBandwidth(), subBody);
            TlvUtil.formatTlv(DOWN_ADJ_THRESHOLD, subBody, body);
        }
        if (aba.getDownAdjustmentThresholdPercentage() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            subBody.writeZero(RESERVED_3);
            writeUint8(subBody, aba.getDownAdjustmentThresholdPercentage().getPercentage());
            writeFloat32(aba.getDownAdjustmentThresholdPercentage().getBandwidth(), subBody);
            TlvUtil.formatTlv(DOWN_ADJ_THRESHOLD_PERCENTAGE, subBody, body);
        }
        if (aba.getMinimumBandwidth() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeFloat32(aba.getMinimumBandwidth().getBandwidth(), subBody);
            TlvUtil.formatTlv(MINIMUN_BANDWIDTH, subBody, body);
        }
        if (aba.getMaximumBandwidth() != null) {
            final ByteBuf subBody = Unpooled.buffer(CONTENT_LENGTH);
            writeFloat32(aba.getMaximumBandwidth().getBandwidth(), subBody);
            TlvUtil.formatTlv(MAXIMUM_BANDWIDTH, subBody, body);
        }
        if (aba.getOverflowThreshold() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            subBody.writeZero(RESERVED_3);
            writeUint8(subBody, aba.getOverflowThreshold().getCount());
            writeFloat32(aba.getOverflowThreshold().getBandwidth(), subBody);
            TlvUtil.formatTlv(OVERFLOW_THRESHOLD, subBody, body);
        }
        if (aba.getOverflowThresholdPercentage() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            // Percentage is encoded on 7 bits only see RFC733 section 5.2.5.2
            subBody.writeByte(aba.getOverflowThresholdPercentage().getPercentage().intValue() << 1);
            subBody.writeZero(RESERVED_2);
            writeUint8(subBody, aba.getOverflowThresholdPercentage().getCount());
            writeFloat32(aba.getOverflowThresholdPercentage().getBandwidth(), subBody);
            TlvUtil.formatTlv(OVERFLOW_THRESHOLD_PERCENTAGE, subBody, body);
        }
        if (aba.getUnderflowThreshold() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            subBody.writeZero(RESERVED_3);
            writeUint8(subBody, aba.getUnderflowThreshold().getCount());
            writeFloat32(aba.getUnderflowThreshold().getBandwidth(), subBody);
            TlvUtil.formatTlv(UNDERFLOW_THRESHOLD, subBody, body);
        }
        if (aba.getUnderflowThresholdPercentage() != null) {
            final ByteBuf subBody = Unpooled.buffer(DOUBLE_CONTENT_LENGTH);
            // Percentage is encoded on 7 bits only see RFC733 section 5.2.5.4
            subBody.writeByte(aba.getUnderflowThresholdPercentage().getPercentage().intValue() << 1);
            subBody.writeZero(RESERVED_2);
            writeUint8(subBody, aba.getUnderflowThresholdPercentage().getCount());
            writeFloat32(aba.getUnderflowThresholdPercentage().getBandwidth(), subBody);
            TlvUtil.formatTlv(UNDERFLOW_THRESHOLD_PERCENTAGE, subBody, body);
        }
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        return parseSubTlvs(buffer);
    }

    private static AutoBandwidthAttributes parseSubTlvs(final ByteBuf buffer) {
        final AutoBandwidthAttributesBuilder abaBuilder = new AutoBandwidthAttributesBuilder();
        while (buffer.isReadable()) {
            var type = buffer.readUnsignedShort();
            var length = buffer.readUnsignedShort();
            Uint32 value;
            Bandwidth bw;
            switch (type) {
                case SAMPLE_INTERVAL -> {
                    value = getUint32(buffer, length);
                    if (value != null) {
                        abaBuilder.setSampleInterval(new SampleIntervalBuilder().setInterval(value).build());
                    }
                }
                case ADJUSTMENT_INTERVAL -> {
                    value = getUint32(buffer, length);
                    if (value != null) {
                        abaBuilder.setAdjustmentInterval(new AdjustmentIntervalBuilder().setAdjustment(value).build());
                    }
                }
                case DOWN_ADJ_INTERVAL -> {
                    value = getUint32(buffer, length);
                    if (value != null) {
                        abaBuilder.setDownAdjustmentInterval(
                            new DownAdjustmentIntervalBuilder().setDownAdjustment(value).build());
                    }
                }
                case ADJUSTMENT_THRESHOLD -> {
                    bw = getBandwidth(buffer, length);
                    if (bw != null) {
                        abaBuilder.setAdjustmentThreshold(new AdjustmentThresholdBuilder().setBandwidth(bw).build());
                    }
                }
                case ADJ_THRESHOLD_PERCENTAGE -> getAdjThresholdPercentage(buffer, length, abaBuilder);
                case DOWN_ADJ_THRESHOLD -> {
                    bw = getBandwidth(buffer, length);
                    if (bw != null) {
                        abaBuilder.setDownAdjustmentThreshold(
                            new DownAdjustmentThresholdBuilder().setBandwidth(bw).build());
                    }
                }
                case DOWN_ADJ_THRESHOLD_PERCENTAGE -> getDownAdjThresholdPercentage(buffer, length, abaBuilder);
                case MINIMUN_BANDWIDTH -> {
                    bw = getBandwidth(buffer, length);
                    if (bw != null) {
                        abaBuilder.setMinimumBandwidth(new MinimumBandwidthBuilder().setBandwidth(bw).build());
                    }
                }
                case MAXIMUM_BANDWIDTH -> {
                    bw = getBandwidth(buffer, length);
                    if (bw != null) {
                        abaBuilder.setMaximumBandwidth(new MaximumBandwidthBuilder().setBandwidth(bw).build());
                    }
                }
                case OVERFLOW_THRESHOLD -> getOverflowThreshold(buffer, length, abaBuilder);
                case OVERFLOW_THRESHOLD_PERCENTAGE -> getOverflowThresholdPercentage(buffer, length, abaBuilder);
                case UNDERFLOW_THRESHOLD -> getUnderflowThreshold(buffer, length, abaBuilder);
                case UNDERFLOW_THRESHOLD_PERCENTAGE -> getUnderflowThresholdPercentage(buffer, length, abaBuilder);
                default -> buffer.skipBytes(length);
            }
        }
        return abaBuilder.build();
    }

    private static Uint32 getUint32(final ByteBuf buffer, final int length) {
        if (length != CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return null;
        }
        return readUint32(buffer);
    }

    private static Bandwidth getBandwidth(final ByteBuf buffer, final int length) {
        if (length != CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return null;
        }
        return new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH));
    }

    private static void getAdjThresholdPercentage(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        buffer.skipBytes(RESERVED_3);
        builder.setAdjustmentThresholdPercentage(new AdjustmentThresholdPercentageBuilder()
            .setPercentage(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }

    private static void getDownAdjThresholdPercentage(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        buffer.skipBytes(RESERVED_3);
        builder.setDownAdjustmentThresholdPercentage(new DownAdjustmentThresholdPercentageBuilder()
            .setPercentage(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }

    private static void getOverflowThreshold(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        buffer.skipBytes(RESERVED_3);
        builder.setOverflowThreshold(new OverflowThresholdBuilder()
            .setCount(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }

    private static void getOverflowThresholdPercentage(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        final var percentage = Uint8.valueOf(buffer.readByte() >> 1);
        buffer.skipBytes(RESERVED_2);
        builder.setOverflowThresholdPercentage(new OverflowThresholdPercentageBuilder()
            .setPercentage(percentage)
            .setCount(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }

    private static void getUnderflowThreshold(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        buffer.skipBytes(RESERVED_3);
        builder.setUnderflowThreshold(new UnderflowThresholdBuilder()
            .setCount(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }

    private static void getUnderflowThresholdPercentage(final ByteBuf buffer, final int length,
            AutoBandwidthAttributesBuilder builder) {
        if (length != DOUBLE_CONTENT_LENGTH) {
            buffer.skipBytes(length);
            return;
        }
        final var percentage = Uint8.valueOf(buffer.readByte() >> 1);
        buffer.skipBytes(RESERVED_2);
        builder.setUnderflowThresholdPercentage(new UnderflowThresholdPercentageBuilder()
            .setPercentage(percentage)
            .setCount(readUint8(buffer))
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, CONTENT_LENGTH)))
            .build());
    }
}
