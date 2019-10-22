/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.SubobjectBuilder;

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
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final List<Subobject> subs = new ArrayList<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit
                .route.object.ero.Subobject s : parseSubobjects(bytes)) {
            subs.add(new SubobjectBuilder()
                .setLoose(s.isLoose())
                .setSubobjectType(s.getSubobjectType())
                .build());
        }
        final IroBuilder builder = new IroBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setSubobject(subs);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Iro,
            "Wrong instance of PCEPObject. Passed %s. Needed IroObject.", object.getClass());
        final Iro iro = ((Iro) object);
        final ByteBuf body = Unpooled.buffer();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit
            .route.object.ero.Subobject> subs = new ArrayList<>();
        for (final Subobject s : iro.getSubobject()) {
            subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                .explicit.route.object.ero.SubobjectBuilder().setLoose(
                s.isLoose()).setSubobjectType(s.getSubobjectType()).build());
        }
        serializeSubobject(subs, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
