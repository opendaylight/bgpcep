/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.BITSET_LENGTH;
import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.F_FLAG_POSITION;
import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.MINIMAL_LENGTH;
import static org.opendaylight.protocol.pcep.segment.routing.SrSubobjectParserUtil.S_FLAG_POSITION;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.SrRroSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;

public class SrRroSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 6;

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrRroSubobject,
                "Unknown subobject instance. Passed %s. Needed SrRroSubobject.", subobject.getSubobjectType()
                        .getClass());

        final SrRroSubobject srRroSubobject = (SrRroSubobject) subobject.getSubobjectType();
        final BitSet bits = new BitSet(BITSET_LENGTH);
        if (srRroSubobject.getSid() == null) {
            bits.set(S_FLAG_POSITION);
        }
        if (srRroSubobject.getNai() == null) {
            bits.set(F_FLAG_POSITION);
        }
        final ByteBuf body = SrSubobjectParserUtil.serializeSrSubobject(srRroSubobject, bits);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() <= MINIMAL_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }

        final SrSubobject srSubobject = SrSubobjectParserUtil.parseSrSubobject(buffer, new Function<BitSet, Void>() {
            @Override
            public Void apply(final BitSet input) {
                return null;
            }
        });
        final SrRroTypeBuilder srRroSubobjectBuilder = new SrRroTypeBuilder(srSubobject);

        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setSubobjectType(srRroSubobjectBuilder.build());
        return subobjectBuilder.build();
    }
}
