/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;

/**
 * Parser for {@link AsNumberCase}.
 */
public class EROAsNumberSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 32;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        return new SubobjectBuilder().setLoose(loose).setSubobjectType(AsNumberCaseParser.parseSubobject(buffer))
                .build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof AsNumberCase,
                "Unknown subobject instance. Passed %s. Needed AsNumberCase.", subobject.getSubobjectType().getClass());
        final ByteBuf body = AsNumberCaseParser.serializeSubobject((AsNumberCase) subobject.getSubobjectType());
        EROSubobjectUtil.formatSubobject(TYPE, subobject.getLoose(), body, buffer);
    }
}
