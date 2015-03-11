/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.BITSET_LENGTH;
import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.MINIMAL_LENGTH;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;

public class SrRroTypeSrSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 6;

    private static final int M_FLAG_POSITION = 7;
    private static final int C_FLAG_POSITION = 6;
    private static final int S_FLAG_POSITION = 5;
    private static final int F_FLAG_POSITION = 4;
    private static final int MPLS_LABEL_OFFSET = 12;

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrSubobject,
                "Unknown subobject instance. Passed %s. Needed SrSubobject.", subobject.getSubobjectType()
                        .getClass());

        final SrSubobject srSubobject = (SrSubobject) subobject.getSubobjectType();
        final SrRroTypeBuilder builder = new SrRroTypeBuilder(srSubobject);
        if (srSubobject.isCFlag() != null && srSubobject.isMFlag() && srSubobject.getSid() != null) {
            builder.setSid(srSubobject.getSid() << MPLS_LABEL_OFFSET);
        }
        final BitArray bits = new BitArray(BITSET_LENGTH);
        bits.set(M_FLAG_POSITION, srSubobject.isMFlag());
        bits.set(C_FLAG_POSITION, srSubobject.isCFlag());
        if (srSubobject.getSid() == null) {
            bits.set(S_FLAG_POSITION, Boolean.TRUE);
        }
        if (srSubobject.getNai() == null) {
            bits.set(F_FLAG_POSITION, Boolean.TRUE);
        }
        final ByteBuf body = SrSubobjectParserUtil.serializeSrSubobject(builder.build(), bits);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() <= MINIMAL_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }
        final BitArray flags = new BitArray(BITSET_LENGTH);
        final SrSubobject srSubobject = SrSubobjectParserUtil.parseSrSubobject(buffer, new Function<BitArray, Void>() {
            @Override
            public Void apply(final BitArray input) {
                flags.set(C_FLAG_POSITION, input.get(C_FLAG_POSITION));
                flags.set(M_FLAG_POSITION, input.get(M_FLAG_POSITION));
                return null;
            }
        }, F_FLAG_POSITION, S_FLAG_POSITION, C_FLAG_POSITION, M_FLAG_POSITION);
        final SrRroTypeBuilder srRroTypeBuilder = new SrRroTypeBuilder(srSubobject);
        srRroTypeBuilder.setCFlag(flags.get(C_FLAG_POSITION));
        srRroTypeBuilder.setMFlag(flags.get(M_FLAG_POSITION));
        if (srRroTypeBuilder.isMFlag() != null && srRroTypeBuilder.isMFlag() && srRroTypeBuilder.getSid() != null) {
            srRroTypeBuilder.setSid(srRroTypeBuilder.getSid() >> MPLS_LABEL_OFFSET);
        }
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setSubobjectType(srRroTypeBuilder.build());
        return subobjectBuilder.build();
    }
}
