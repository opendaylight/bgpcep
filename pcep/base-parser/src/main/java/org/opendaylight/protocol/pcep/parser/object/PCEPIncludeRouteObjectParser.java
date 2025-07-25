/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.include.route.object.iro.SubobjectBuilder;

/**
 * Parser for {@link Iro}.
 */
public final class PCEPIncludeRouteObjectParser extends AbstractEROWithSubobjectsParser {

    private static final int CLASS = 10;
    private static final int TYPE = 1;

    public PCEPIncludeRouteObjectParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg, CLASS, TYPE);
    }

    @Override
    public Iro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final List<Subobject> subs = new ArrayList<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit
                .route.object.ero.Subobject s : parseSubobjects(bytes)) {
            subs.add(new SubobjectBuilder()
                .setLoose(s.getLoose())
                .setSubobjectType(s.getSubobjectType())
                .build());
        }
        return new IroBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule())
                .setSubobject(subs)
                .build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Iro, "Wrong instance of PCEPObject. Passed %s. Needed IroObject.",
            object.getClass());
        final Iro iro = (Iro) object;
        final ByteBuf body = Unpooled.buffer();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit
            .route.object.ero.Subobject> subs = new ArrayList<>();
        for (final Subobject s : iro.nonnullSubobject()) {
            subs.add(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602
                    .explicit.route.object.ero.SubobjectBuilder()
                        .setLoose(s.getLoose())
                        .setSubobjectType(s.getSubobjectType())
                        .build());
        }
        serializeSubobject(subs, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
