/*
 * Copyright (c) 2025 Orange.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.Srv6Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.Srv6EroTypeBuilder;

public class Srv6EroSubobjectParser extends AbstractSrv6SubobjectParser implements EROSubobjectParser,
        EROSubobjectSerializer {

    public static final int TYPE = 40;

    Srv6EroSubobjectParser() {
        // Hidden on purpose
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof Srv6Subobject,
                "Unknown subobject instance. Passed %s. Needed Srv6Subobject.", subobject.getSubobjectType()
                        .getClass());

        final Srv6Subobject srv6Subobject = (Srv6Subobject) subobject.getSubobjectType();
        final ByteBuf body = serializeSubobject(srv6Subobject);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.getLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        return new SubobjectBuilder()
            .setLoose(loose)
            .setSubobjectType(new Srv6EroTypeBuilder(parseSrv6Subobject(buffer)).build())
            .build();
    }
}