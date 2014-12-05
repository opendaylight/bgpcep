/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.BITSET_LENGTH;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrEroSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;

public class SrEroSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 5;

    private static final int M_FLAG_POSITION = 7;
    private static final int C_FLAG_POSITION = 6;
    private static final int S_FLAG_POSITION = 5;
    private static final int F_FLAG_POSITION = 4;
    private static final int MPLS_LABEL_OFFSET = 12;

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrEroSubobject,
                "Unknown subobject instance. Passed %s. Needed SrEroSubobject.", subobject.getSubobjectType()
                        .getClass());

        final SrEroSubobject srEroSubobject = (SrEroSubobject) subobject.getSubobjectType();
        final SrEroTypeBuilder builder = new SrEroTypeBuilder(srEroSubobject);
        if (srEroSubobject.isMFlag() != null && srEroSubobject.isMFlag() && srEroSubobject.getSid() != null) {
            builder.setSid(srEroSubobject.getSid() << MPLS_LABEL_OFFSET);
        }

        final BitSet bits = new BitSet(BITSET_LENGTH);
        if (srEroSubobject.isMFlag() != null) {
            bits.set(M_FLAG_POSITION, srEroSubobject.isMFlag());
        }
        if (srEroSubobject.isCFlags() != null) {
            bits.set(C_FLAG_POSITION, srEroSubobject.isCFlags());
        }
        if (srEroSubobject.getSid() == null) {
            bits.set(S_FLAG_POSITION);
        }
        if (srEroSubobject.getNai() == null) {
            bits.set(F_FLAG_POSITION);
        }
        final ByteBuf body = SrSubobjectParserUtil.serializeSrSubobject(builder.build(), bits);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() <= SrSubobjectParserUtil.MINIMAL_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }

        final BitSet flags = new BitSet(BITSET_LENGTH);
        final SrSubobject srSubobject = SrSubobjectParserUtil.parseSrSubobject(buffer, new Function<BitSet, Void>() {
            @Override
            public Void apply(final BitSet input) {
                flags.set(C_FLAG_POSITION, input.get(C_FLAG_POSITION));
                flags.set(M_FLAG_POSITION, input.get(M_FLAG_POSITION));
                return null;
            }
        }, F_FLAG_POSITION, S_FLAG_POSITION);
        final SrEroTypeBuilder srEroSubobjectBuilder = new SrEroTypeBuilder(srSubobject);
        srEroSubobjectBuilder.setCFlags(flags.get(C_FLAG_POSITION));
        srEroSubobjectBuilder.setMFlag(flags.get(M_FLAG_POSITION));
        if (srEroSubobjectBuilder.isMFlag() != null && srEroSubobjectBuilder.isMFlag() && srEroSubobjectBuilder.getSid() != null) {
            srEroSubobjectBuilder.setSid(srEroSubobjectBuilder.getSid() >> MPLS_LABEL_OFFSET);
        }

        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setLoose(loose);
        subobjectBuilder.setSubobjectType(srEroSubobjectBuilder.build());
        return subobjectBuilder.build();
    }

}