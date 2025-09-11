/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.BindingType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.BindingValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.FlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6Behavior;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6BehaviorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6Builder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link PathBinding}.
 */
public final class PathBindingTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 55;

    // TLV and FLAGS definition
    private static final int RESERVED = 2;
    private static final int FLAGS_SIZE = 8;
    private static final int R_FLAG = 0;
    private static final int S_FLAG = 1;

    // MPLS Label Entry
    private static final int LABEL_MASK = 0xfffff;
    private static final int TC_MASK = 0x7;
    private static final int S_MASK = 0x1;
    private static final int TTL_MASK = 0xff;
    private static final int LABEL_SHIFT = 12;
    private static final int TC_SHIFT = LABEL_SHIFT - 3;
    private static final int S_SHIFT = TC_SHIFT - 1;


    private static final Map<BindingType, PathBindingTlvCodec> BT_PARSERS;

    static {
        final Builder<BindingType, PathBindingTlvCodec> parsers = ImmutableMap.builder();
        parsers.put(BindingType.MplsLabel, new MplsLabelCodec());
        parsers.put(BindingType.MplsLabelEntry, new MplsLabelEntryCodec());
        parsers.put(BindingType.Srv6, new Srv6Codec());
        parsers.put(BindingType.Srv6Behavior, new Srv6BehaviorCodec());
        BT_PARSERS = parsers.build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof PathBinding, "The TLV must be PathBinding type, but was %s", tlv.getClass());
        final PathBinding pTlv = (PathBinding) tlv;
        checkArgument(pTlv.getBindingValue() != null, "Missing Binding Value in Path Binding TLV: %s", pTlv);
        final ByteBuf body = Unpooled.buffer();
        final var type = pTlv.getBindingType();
        body.writeByte(type.getIntValue());
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(R_FLAG, pTlv.getFlags().getRemoval());
        bs.set(S_FLAG, pTlv.getFlags().getSpecified());
        bs.toByteBuf(body);
        body.writeZero(RESERVED);
        final PathBindingTlvCodec codec = BT_PARSERS.get(type);
        checkArgument(codec != null, "Unsupported Path Binding Type: %s", type);
        codec.writeEntry(body, pTlv.getBindingValue());

        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < 7) {
            throw new PCEPDeserializerException("Wrong Buffer Size Passed: " + buffer.readableBytes()
                + "; Expected at least 7.");
        }
        final BindingType type = BindingType.forValue(buffer.readByte());
        if (type == null) {
            throw new PCEPDeserializerException("Unsupported Path Binding Type");
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        buffer.skipBytes(RESERVED);
        final PathBindingTlvCodec codec = BT_PARSERS.get(type);
        return new PathBindingBuilder()
            .setFlags(new FlagsBuilder().setRemoval(flags.get(R_FLAG)).setSpecified(flags.get(S_FLAG)).build())
            .setBindingType(type)
            .setBindingValue(codec.readEntry(buffer))
            .build();
    }

    private static final class MplsLabelCodec extends PathBindingTlvCodec {
        MplsLabelCodec() {
            // no-op
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingValue bindingValue) {
            checkArgument(bindingValue instanceof MplsLabel, "Binding Value is not MplsLabel");
            final MplsLabel mplsLabel = (MplsLabel) bindingValue;
            ByteBufUtils.write(buf, Uint32.valueOf(getMplsStackEntry(mplsLabel.getMplsLabel())));
            /*
             *  Shift buffer by One Byte as MPLS Label will be padded latter and MUST NOT be count in the TLV Length
             *  as per https://www.rfc-editor.org/rfc/rfc9604.html#section-4 BT=0
             */
            buf.writerIndex(buf.writerIndex() - 1);
        }

        @Override
        BindingValue readEntry(final ByteBuf buffer) {
            return new MplsLabelBuilder().setMplsLabel(getMplsLabel(buffer.readUnsignedInt())).build();
        }
    }

    private static final class MplsLabelEntryCodec extends PathBindingTlvCodec {
        MplsLabelEntryCodec() {
            // no-op
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingValue bindingValue) {
            checkArgument(bindingValue instanceof MplsLabelEntry, "Binding Value is not MplsLabelEntry");
            final MplsLabelEntry mplsEntry = (MplsLabelEntry) bindingValue;
            final long entry = getMplsStackEntry(mplsEntry.getLabel())
                    | mplsEntry.getTrafficClass().toJava() << TC_SHIFT
                    | (mplsEntry.getBottomOfStack() ? 1 : 0) << S_SHIFT
                    | mplsEntry.getTimeToLive().toJava();
            ByteBufUtils.write(buf, Uint32.valueOf(entry));
        }

        @Override
        BindingValue readEntry(final ByteBuf buffer) {
            final long entry = buffer.readUnsignedInt();
            return new MplsLabelEntryBuilder()
                    .setLabel(getMplsLabel(entry))
                    .setTrafficClass(Uint8.valueOf(entry >> TC_SHIFT & TC_MASK))
                    .setBottomOfStack((entry >> S_SHIFT & S_MASK) == 1)
                    .setTimeToLive(Uint8.valueOf(entry & TTL_MASK))
                    .build();
        }
    }

    private static final class Srv6Codec extends PathBindingTlvCodec {
        Srv6Codec() {
            // no-op
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingValue bindingValue) {
            checkArgument(bindingValue instanceof Srv6, "Binding Value is not Srv6");
            final Srv6 srv6 = (Srv6) bindingValue;
            Ipv6Util.writeIpv6Address(srv6.getSrv6Address(), buf);
        }

        @Override
        BindingValue readEntry(final ByteBuf buffer) {
            return new Srv6Builder().setSrv6Address(new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer))).build();
        }
    }

    private static final class Srv6BehaviorCodec extends PathBindingTlvCodec {
        Srv6BehaviorCodec() {
            // no-op
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingValue bindingValue) {
            checkArgument(bindingValue instanceof Srv6Behavior, "Binding Value is not an Srv6 Behavior");
            final Srv6Behavior srv6Behavior = (Srv6Behavior) bindingValue;
            Ipv6Util.writeIpv6Address(srv6Behavior.getSrv6Sid(), buf);
            buf.writeZero(RESERVED);
            ByteBufUtils.writeUint16(buf, srv6Behavior.getEndpointBehavior());
            ByteBufUtils.writeUint8(buf, srv6Behavior.getLocatorBlockLength());
            ByteBufUtils.writeUint8(buf, srv6Behavior.getLocatorNodeLength());
            ByteBufUtils.writeUint8(buf, srv6Behavior.getFunctionLength());
            ByteBufUtils.writeUint8(buf, srv6Behavior.getArgumentLength());
        }

        @Override
        BindingValue readEntry(final ByteBuf buffer) {
            final var srv6Behavior = new Srv6BehaviorBuilder();
            srv6Behavior.setSrv6Sid(new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer)));
            buffer.skipBytes(RESERVED);
            srv6Behavior.setEndpointBehavior(ByteBufUtils.readUint16(buffer))
                .setLocatorBlockLength(ByteBufUtils.readUint8(buffer))
                .setLocatorNodeLength(ByteBufUtils.readUint8(buffer))
                .setFunctionLength(ByteBufUtils.readUint8(buffer))
                .setArgumentLength(ByteBufUtils.readUint8(buffer));
            return srv6Behavior.build();
        }
    }

    private abstract static class PathBindingTlvCodec {
        abstract BindingValue readEntry(ByteBuf buf);

        abstract void writeEntry(ByteBuf buf, BindingValue value);
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel
        getMplsLabel(final long mplsStackEntry) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125
                .MplsLabel(Uint32.valueOf(mplsStackEntry >> LABEL_SHIFT & LABEL_MASK));
    }

    private static long getMplsStackEntry(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network
            .concepts.rev131125.MplsLabel mplsLabel) {
        return mplsLabel.getValue().toJava() << LABEL_SHIFT;
    }
}
