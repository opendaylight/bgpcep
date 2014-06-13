/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder;

/**
 * Parser for {@link Iro}
 */
public class PCEPIncludeRouteObjectParser extends AbstractEROWithSubobjectsParser {

    public static final int CLASS = 10;

    public static final int TYPE = 1;

    public PCEPIncludeRouteObjectParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    public Iro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final IroBuilder builder = new IroBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final List<Subobject> subs = new ArrayList<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject s : parseSubobjects(bytes)) {
            subs.add(new SubobjectBuilder().setSubobjectType(s.getSubobjectType()).build());
        }
        builder.setSubobject(subs);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        if (!(object instanceof Iro)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed IncludeRouteObject.");
        }
        final Iro iro = ((Iro) object);

        assert !(iro.getSubobject().isEmpty()) : "Empty Include Route Object.";

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> subs = Lists.newArrayList();

        for (final Subobject s : iro.getSubobject()) {
            subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setLoose(
                    false).setSubobjectType(s.getSubobjectType()).build());
        }
	// FIXME: switch to ByteBuf
        buffer.writeBytes(ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), serializeSubobject(subs)));
    }
}
