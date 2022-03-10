/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.path.binding.BindingTypeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.path.binding.binding.type.value.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.path.binding.binding.type.value.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.path.binding.binding.type.value.MplsLabelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.path.binding.tlv.path.binding.binding.type.value.MplsLabelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link PathBinding}.
 */
public final class PathBindingTlvParser implements TlvParser, TlvSerializer {
    // TODO: to be confirmed by IANA
    public static final int TYPE = 31;

    private static final Uint16 MPLS_LABEL = Uint16.ZERO;
    private static final Uint16 MPLS_STACK_ENTRY = Uint16.ONE;

    private static final int LABEL_MASK = 0xfffff;
    private static final int TC_MASK = 0x7;
    private static final int S_MASK = 0x1;
    private static final int TTL_MASK = 0xff;
    private static final int LABEL_SHIFT = 12;
    private static final int TC_SHIFT = LABEL_SHIFT - 3;
    private static final int S_SHIFT = TC_SHIFT - 1;
    // 2 bytes type + 4 bytes binding
    private static final int MPLS_BINDING_LENGTH = 6;

    private static final Map<Uint16, PathBindingTlvCodec> BT_PARSERS;
    private static final Map<Class<? extends BindingTypeValue>, PathBindingTlvCodec> BT_SERIALIZERS;

    static {
        final MplsLabelCodec mplsLabelCodec = new MplsLabelCodec();
        final MplsLabelEntryCodec mplsLabelEntryCodec = new MplsLabelEntryCodec();
        final Builder<Uint16, PathBindingTlvCodec> parsers = ImmutableMap.builder();
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
        checkArgument(tlv instanceof PathBinding, "The TLV must be PathBinding type, but was %s", tlv.getClass());
        final PathBinding pTlv = (PathBinding) tlv;
        final BindingTypeValue bindingTypeValue = pTlv.getBindingTypeValue();
        checkArgument(bindingTypeValue != null, "Missing Binding Value in Path Bidning TLV: %s", pTlv);
        final ByteBuf body = Unpooled.buffer(MPLS_BINDING_LENGTH);
        final PathBindingTlvCodec codec = BT_SERIALIZERS.get(bindingTypeValue.implementedInterface());
        checkArgument(codec != null, "Unsupported Path Binding Type: %s", bindingTypeValue.implementedInterface());
        codec.writeBinding(body, bindingTypeValue);

        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final Uint16 type = ByteBufUtils.readUint16(buffer);
        final PathBindingTlvCodec codec = BT_PARSERS.get(type);
        if (codec == null) {
            throw new PCEPDeserializerException("Unsupported Path Binding Type: " + type);
        }
        return new PathBindingBuilder().setBindingTypeValue(codec.readEntry(buffer)).build();
    }

    private static final class MplsLabelCodec extends PathBindingTlvCodec {
        MplsLabelCodec() {
            super(MPLS_LABEL);
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingTypeValue bindingValue) {
            checkArgument(bindingValue instanceof MplsLabel, "bindingValue is not MplsLabel");
            final MplsLabel mplsLabel = (MplsLabel) bindingValue;
            ByteBufUtils.write(buf, Uint32.valueOf(getMplsStackEntry(mplsLabel.getMplsLabel())));
        }

        @Override
        BindingTypeValue readEntry(final ByteBuf buffer) {
            return new MplsLabelBuilder().setMplsLabel(getMplsLabel(buffer.readUnsignedInt())).build();
        }
    }

    private static final class MplsLabelEntryCodec extends PathBindingTlvCodec {
        MplsLabelEntryCodec() {
            super(MPLS_STACK_ENTRY);
        }

        @Override
        void writeEntry(final ByteBuf buf, final BindingTypeValue bindingValue) {
            checkArgument(bindingValue instanceof MplsLabelEntry, "bindingValue is not MplsLabelEntry");
            final MplsLabelEntry mplsEntry = (MplsLabelEntry) bindingValue;
            final long entry = getMplsStackEntry(mplsEntry.getLabel())
                    | mplsEntry.getTrafficClass().toJava() << TC_SHIFT
                    | (mplsEntry.getBottomOfStack() ? 1 : 0) << S_SHIFT
                    | mplsEntry.getTimeToLive().toJava();
            ByteBufUtils.write(buf, Uint32.valueOf(entry));
        }

        @Override
        BindingTypeValue readEntry(final ByteBuf buffer) {
            final long entry = buffer.readUnsignedInt();
            return new MplsLabelEntryBuilder()
                    .setLabel(getMplsLabel(entry))
                    .setTrafficClass(Uint8.valueOf(entry >> TC_SHIFT & TC_MASK))
                    .setBottomOfStack((entry >> S_SHIFT & S_MASK) == 1)
                    .setTimeToLive(Uint8.valueOf(entry & TTL_MASK))
                    .build();
        }
    }

    private abstract static class PathBindingTlvCodec {
        private final @NonNull Uint16 bindingType;

        PathBindingTlvCodec(final Uint16 bindingType) {
            this.bindingType = requireNonNull(bindingType);
        }

        final @NonNull Uint16 getBindingType() {
            return bindingType;
        }

        final void writeBinding(final ByteBuf buf, final BindingTypeValue binding) {
            ByteBufUtils.writeMandatory(buf, bindingType, "bindingType");
            writeEntry(buf, binding);
        }

        abstract BindingTypeValue readEntry(ByteBuf buf);

        abstract void writeEntry(ByteBuf buf, BindingTypeValue value);
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
