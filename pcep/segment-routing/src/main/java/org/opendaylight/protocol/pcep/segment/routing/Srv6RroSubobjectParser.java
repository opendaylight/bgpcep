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
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.Srv6Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.rro.subobject.subobject.type.Srv6RroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.SubobjectType;

public class Srv6RroSubobjectParser extends AbstractSrv6SubobjectParser implements RROSubobjectParser,
        RROSubobjectSerializer {

    public static final int TYPE = 40;

    Srv6RroSubobjectParser() {
        // Hidden on purpose
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final SubobjectType subobjType = subobject.getSubobjectType();
        checkArgument(subobjType instanceof Srv6Subobject,
            "Unknown subobject instance. Passed %s. Needed Srv6Subobject.", subobjType.getClass());
        final Srv6Subobject srv6Subobject = (Srv6Subobject) subobjType;
        final ByteBuf body = serializeSubobject(srv6Subobject);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        return new SubobjectBuilder()
            .setSubobjectType(new Srv6RroTypeBuilder(parseSrv6Subobject(buffer)).build())
            .build();
    }
}
