/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.path.binding.BindingTypeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.path.binding.binding.type.value.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.path.binding.binding.type.value.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.path.binding.binding.type.value.MplsLabelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.path.binding.binding.type.value.MplsLabelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yangtools.concepts.Codec;

/**
 * Parser for {@link PathBinding}
 */
public final class PathBindingTlvParser implements TlvParser, TlvSerializer {

    // TODO: to be confirmed by IANA
    public static final int TYPE = 31;

    private static final int MPLS_LABEL = 0;
    private static final int MPLS_STACK_ENTRY = 1;

    private static final int LABEL_MASK = 0xfffff;
    private static final int TC_MASK = 0x7;
    private static final int S_MASK = 0x1;
    private static final int TTL_MASK = 0xff;
    private static final int LABEL_SHIFT = 12;
    private static final int TC_SHIFT = LABEL_SHIFT - 3;
    private static final int S_SHIFT = TC_SHIFT - 1;
    private static final int MPLS_ENTRY_LENGTH = 4;
    private static final int MPLS_BINDING_LENGTH = MPLS_ENTRY_LENGTH + 2;

    private static final Map<Integer, PathBindingTlvCodec> BT_PARSERS;
    private static final Map<Class<? extends BindingTypeValue>, PathBindingTlvCodec> BT_SERIALIZERS;

    static {
        final MplsLabelCodec mplsLabelCodec = new MplsLabelCodec();
        final MplsLabelEntryCodec mplsLabelEntryCodec = new MplsLabelEntryCodec();
        final Builder<Integer, PathBindingTlvCodec> parsers = ImmutableMap.builder();
        final Builder<Class<? extends BindingTypeValue>, PathBindingTlvCodec> serializers =
                ImmutableMap.builder();

        parsers.put(mplsLabelCodec.getBindingType(), mplsLabelCodec);
        serializers.put(MplsLabel.class, mplsLabelCodec);

        parsers.put(mplsLabelEntryCodec.getBindingType(), mplsLabelEntryCodec);
        serializers.put(MplsLabelEntry.class, mplsLabelEntryCodec);

        BT_PARSERS = parsers.build();
        BT_SERIALIZERS = serializers.build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof PathBinding,
            "The TLV must be PathBinding type, but was %s", tlv.getClass());
        final PathBinding pTlv = (PathBinding) tlv;
        final BindingTypeValue bindingTypeValue = pTlv.getBindingTypeValue();
        Preconditions.checkArgument(bindingTypeValue != null,
            "Missing Binding Value in Path Bidning TLV: %s", pTlv);
        final ByteBuf body = Unpooled.buffer(MPLS_BINDING_LENGTH);
        final PathBindingTlvCodec codec = BT_SERIALIZERS.get(bindingTypeValue.getImplementedInterface());
        Preconditions.checkArgument(codec != null,
            "Unsupported Path Binding Type: %s", bindingTypeValue.getImplementedInterface());
        ByteBufWriteUtil.writeUnsignedShort(codec.getBindingType(), body);
        body.writeBytes(codec.serialize(bindingTypeValue));

        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final int type = buffer.readUnsignedShort();
        final PathBindingTlvCodec codec = BT_PARSERS.get(type);
        if (codec == null) {
            throw new PCEPDeserializerException("Unsupported Path Binding Type: " + type);
        }
        final PathBindingBuilder builder = new PathBindingBuilder();
        return builder.setBindingTypeValue(codec.deserialize(buffer)).build();
    }

    private static final class MplsLabelCodec implements PathBindingTlvCodec {

        @Override
        public ByteBuf serialize(final BindingTypeValue bindingValue) {
            final MplsLabel mplsLabel = (MplsLabel) bindingValue;
            final ByteBuf value = Unpooled.buffer(MPLS_ENTRY_LENGTH);
            ByteBufWriteUtil.writeUnsignedInt(getMplsStackEntry(mplsLabel.getMplsLabel()), value);
            return value;
        }

        @Override
        public BindingTypeValue deserialize(final ByteBuf buffer) {
            final MplsLabelBuilder builder = new MplsLabelBuilder();
            builder.setMplsLabel(getMplsLabel(buffer.readUnsignedInt()));
            return builder.build();
        }

        @Override
        public int getBindingType() {
            return MPLS_LABEL;
        }
    }

    private static final class MplsLabelEntryCodec implements PathBindingTlvCodec {

        @Override
        public ByteBuf serialize(final BindingTypeValue bindingValue) {
            final MplsLabelEntry mplsEntry = (MplsLabelEntry) bindingValue;
            final ByteBuf value = Unpooled.buffer(MPLS_ENTRY_LENGTH);
            final long entry = getMplsStackEntry(mplsEntry.getLabel())
                    | mplsEntry.getTrafficClass() << TC_SHIFT
                    | (mplsEntry.isBottomOfStack() ? 1 : 0) << S_SHIFT
                    | mplsEntry.getTimeToLive();
            ByteBufWriteUtil.writeUnsignedInt(entry, value);
            return value;
        }

        @Override
        public BindingTypeValue deserialize(final ByteBuf buffer) {
            final MplsLabelEntryBuilder builder = new MplsLabelEntryBuilder();
            final long entry = buffer.readUnsignedInt();
            builder.setLabel(getMplsLabel(entry));
            builder.setTrafficClass((short) (entry >> TC_SHIFT & TC_MASK));
            builder.setBottomOfStack((entry >> S_SHIFT & S_MASK) == 1);
            builder.setTimeToLive((short) (entry & TTL_MASK));
            return builder.build();
        }

        @Override
        public int getBindingType() {
            return MPLS_STACK_ENTRY;
        }
    }

    private interface PathBindingTlvCodec extends Codec<ByteBuf, BindingTypeValue> {
        int getBindingType();
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel getMplsLabel(
            final long mplsStackEntry) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel(
                mplsStackEntry >> LABEL_SHIFT & LABEL_MASK);
    }

    private static long getMplsStackEntry(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel mplsLabel) {
        return mplsLabel.getValue() << LABEL_SHIFT;
    }

}
