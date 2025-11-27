/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.rro.subobject.subobject.type.SrRroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.SubobjectType;

public class SrRroSubobjectParser extends AbstractSrSubobjectParser implements RROSubobjectParser,
        RROSubobjectSerializer {

    public static final int TYPE = 36;

    public SrRroSubobjectParser() {
        // Hidden on purpose
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final SubobjectType subobjType = subobject.getSubobjectType();
        checkArgument(subobjType instanceof SrSubobject, "Unknown subobject instance. Passed %s. Needed SrSubobject.",
            subobjType.getClass());
        final SrSubobject srSubobject = (SrSubobject) subobjType;
        final ByteBuf body = serializeSubobject(srSubobject);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        return new SubobjectBuilder().setSubobjectType(new SrRroTypeBuilder(parseSrSubobject(buffer)).build()).build();
    }
}
