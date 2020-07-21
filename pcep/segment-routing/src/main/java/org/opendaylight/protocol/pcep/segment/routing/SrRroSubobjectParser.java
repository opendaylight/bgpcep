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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType;

public class SrRroSubobjectParser extends AbstractSrSubobjectParser implements RROSubobjectParser,
        RROSubobjectSerializer {

    @Deprecated
    private static final int LEGACY_TYPE = 6;
    private static final int IANA_TYPE = 36;

    @Deprecated
    private final int type;

    SrRroSubobjectParser() {
        this.type = IANA_TYPE;
    }

    @Deprecated
    SrRroSubobjectParser(final boolean isIanaAssignedType) {
        this.type = isIanaAssignedType ? IANA_TYPE : LEGACY_TYPE;
    }

    @Override
    public void serializeSubobject(Subobject subobject, ByteBuf buffer) {
        final SubobjectType subobjType = subobject.getSubobjectType();
        checkArgument(subobjType instanceof SrSubobject, "Unknown subobject instance. Passed %s. Needed SrSubobject.",
            subobjType.getClass());
        final SrSubobject srSubobject = (SrSubobject) subobjType;
        final ByteBuf body = serializeSubobject(srSubobject);
        RROSubobjectUtil.formatSubobject(this.type, body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        final SrRroTypeBuilder srRroSubobjectBuilder = new SrRroTypeBuilder(parseSrSubobject(buffer));
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setSubobjectType(srRroSubobjectBuilder.build());
        return subobjectBuilder.build();
    }

    @Deprecated
    public int getCodePoint() {
        return this.type;
    }
}
