/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.path.binding.BindingValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.path.binding.binding.value.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.path.binding.binding.value.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.path.binding.binding.value.MplsLabelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.path.binding.tlv.path.binding.binding.value.MplsLabelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link PathBinding}
 */
public class PathBindingTlvParser implements TlvParser, TlvSerializer {

    // TODO: to be confirmed by IANA
    public static final int TYPE = 31;

    private static final int MPLS_LABEL = 0;
    private static final int MPLS_LABEL_STACK = 1;

    private static final int LABEL_MASK = 0xfffff;
    private static final int TC_MASK = 0x7;
    private static final int S_MASK = 0x1;
    private static final int TTL_MASK = 0xff;
    private static final int LABEL_SHIFT = 12;
    private static final int TC_SHIFT = LABEL_SHIFT - 3;
    private static final int S_SHIFT = TC_SHIFT - 1;
    private static final int MPLS_ENTRY_LENGTH = 4;
    private static final int MPLS_BINDING_LENGTH = MPLS_ENTRY_LENGTH + 2;

    protected static final Map<Integer, ? extends Converter<BindingValue, ByteBuf>> REGISTER = ImmutableMap.of(MPLS_LABEL,
            new MplsLabelConverter(), MPLS_LABEL_STACK, new MplsLabelEntryConverter());

    @Override
    public final void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof PathBinding, "PathBinding is mandatory.");
        final PathBinding pTlv = (PathBinding) tlv;
        final int bType = pTlv.getBindingType();
        final Converter<BindingValue, ByteBuf> converter = REGISTER.get(bType);
        Preconditions.checkArgument(converter != null, "Unsupported Path Binding Type: %s", bType);
        final ByteBuf body = Unpooled.buffer(MPLS_BINDING_LENGTH);
        ByteBufWriteUtil.writeUnsignedShort(bType, body);
        body.writeBytes(converter.convert(pTlv.getBindingValue()));
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public final Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        final int type = buffer.readUnsignedShort();
        final Converter<BindingValue, ByteBuf> converter = REGISTER.get(type);
        if (converter == null) {
            throw new PCEPDeserializerException("Unsupported Path Binding Type.");
        }
        return new PathBindingBuilder().setBindingType(type).setBindingValue(converter.reverse().convert(buffer)).build();
    }

    private static final class MplsLabelConverter extends Converter<BindingValue, ByteBuf> {

        @Override
        protected ByteBuf doForward(final BindingValue bindingValue) {
            Preconditions.checkArgument(bindingValue instanceof MplsLabel, "Wrong instance of BindingValue provided.");
            final MplsLabel mplsLabel = (MplsLabel) bindingValue;
            final ByteBuf value = Unpooled.buffer(MPLS_ENTRY_LENGTH);
            ByteBufWriteUtil.writeUnsignedInt(getMplsStackEntry(mplsLabel.getMplsLabel()), value);
            return value;
        }

        @Override
        protected BindingValue doBackward(final ByteBuf buffer) {
            final MplsLabelBuilder builder = new MplsLabelBuilder();
            builder.setMplsLabel(getMplsLabel(buffer.readUnsignedInt()));
            return builder.build();
        }
    }

    private static final class MplsLabelEntryConverter extends Converter<BindingValue, ByteBuf> {

        @Override
        protected ByteBuf doForward(final BindingValue bindingValue) {
            Preconditions.checkArgument(bindingValue instanceof MplsLabelEntry, "Wrong instance of BindingValue provided.");
            final MplsLabelEntry mplsEntry = ((MplsLabelEntry) bindingValue);
            final ByteBuf value = Unpooled.buffer(MPLS_ENTRY_LENGTH);
            final long entry = getMplsStackEntry(mplsEntry.getLabel())
                    | mplsEntry.getTrafficClass() << TC_SHIFT
                    | (mplsEntry.isBottomOfStack() ? 1 : 0) << S_SHIFT
                    | mplsEntry.getTimeToLive();
            ByteBufWriteUtil.writeUnsignedInt(entry, value);
            return value;
        }

        @Override
        protected BindingValue doBackward(final ByteBuf buffer) {
            final MplsLabelEntryBuilder builder = new MplsLabelEntryBuilder();
            final long entry = buffer.readUnsignedInt();
            builder.setLabel(getMplsLabel(entry));
            builder.setTrafficClass((short) ((entry >> TC_SHIFT) & TC_MASK));
            builder.setBottomOfStack(((entry >> S_SHIFT) & S_MASK) == 1 ? Boolean.TRUE : Boolean.FALSE);
            builder.setTimeToLive((short) (entry & TTL_MASK));
            return builder.build();
        }
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel getMplsLabel(
            final long mplsStackEntry) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel(
                (mplsStackEntry >> LABEL_SHIFT) & LABEL_MASK);
    }

    private static long getMplsStackEntry(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel mplsLabel) {
        return mplsLabel.getValue() << LABEL_SHIFT;
    }
}
