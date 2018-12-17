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
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.ExrsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.ExrsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.Exrs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.ExrsBuilder;

public class EROExplicitExclusionRouteSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 33;

    private static final int HEADER_LENGTH = 2;

    private final XROSubobjectRegistry registry;

    public EROExplicitExclusionRouteSubobjectParser(final XROSubobjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(loose);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
            .object.xro.Subobject> list = parseSubobject(buffer);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
            .subobjects.subobject.type.exrs._case.exrs.Exrs> exrss = new ArrayList<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
                .object.xro.Subobject s : list) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
                .subobjects.subobject.type.exrs._case.exrs.ExrsBuilder b = new org.opendaylight.yang.gen.v1.urn
                    .opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case
                    .exrs.ExrsBuilder();
            b.setAttribute(s.getAttribute());
            b.setMandatory(s.isMandatory());
            b.setSubobjectType(s.getSubobjectType());
            exrss.add(b.build());
        }
        builder.setSubobjectType(new ExrsCaseBuilder().setExrs(new ExrsBuilder().setExrs(exrss).build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof ExrsCase,
            "Unknown subobject instance. Passed %s. Needed Exrs.", subobject.getSubobjectType().getClass());
        final Exrs e = ((ExrsCase) subobject.getSubobjectType()).getExrs();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
            .object.xro.Subobject> list = new ArrayList<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
                .subobjects.subobject.type.exrs._case.exrs.Exrs ex : e.getExrs()) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
                .object.xro.SubobjectBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder();
            b.setAttribute(ex.getAttribute());
            b.setMandatory(ex.isMandatory());
            b.setSubobjectType(ex.getSubobjectType());
            list.add(b.build());
        }
        final ByteBuf body = Unpooled.buffer();
        serializeSubobject(list, body);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
            .object.xro.Subobject> parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
            .object.xro.Subobject> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final boolean mandatory = (buffer.getByte(buffer.readerIndex()) & 1 << Values.FIRST_BIT_OFFSET) != 0;
            final int type = buffer.readUnsignedByte() & Values.BYTE_MAX_VALUE_BYTES & ~(1 << Values.FIRST_BIT_OFFSET);
            final int length = buffer.readUnsignedByte() - HEADER_LENGTH;
            if (length > buffer.readableBytes()) {
                throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
                        + buffer.readableBytes());
            }
            subs.add(this.registry.parseSubobject(type, buffer.readSlice(length), mandatory));
        }
        return subs;
    }

    private void serializeSubobject(final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
            .types.rev181109.exclude.route.object.xro.Subobject> subobjects, final ByteBuf body) {
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route
                .object.xro.Subobject subobject : subobjects) {
            this.registry.serializeSubobject(subobject, body);
        }
    }
}
