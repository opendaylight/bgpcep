/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;

public class SrEroSubobjectParser extends AbstractSrSubobjectParser implements EROSubobjectParser,
        EROSubobjectSerializer {

    @Deprecated
    private static final int LEGACY_TYPE = 5;
    private static final int IANA_TYPE = 36;

    @Deprecated
    private final int type;

    SrEroSubobjectParser() {
        type = IANA_TYPE;
    }

    @Deprecated
    SrEroSubobjectParser(final boolean isIanaAssignedType) {
        type = isIanaAssignedType ? IANA_TYPE : LEGACY_TYPE;
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof SrSubobject,
                "Unknown subobject instance. Passed %s. Needed SrSubobject.", subobject.getSubobjectType()
                        .getClass());

        final SrSubobject srSubobject = (SrSubobject) subobject.getSubobjectType();
        final ByteBuf body = serializeSubobject(srSubobject);
        EROSubobjectUtil.formatSubobject(type, subobject.getLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        final SrEroTypeBuilder srEroSubobjectBuilder = new SrEroTypeBuilder(parseSrSubobject(buffer));
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setLoose(loose);
        subobjectBuilder.setSubobjectType(srEroSubobjectBuilder.build());
        return subobjectBuilder.build();
    }

    @Deprecated
    public int getCodePoint() {
        return type;
    }
}