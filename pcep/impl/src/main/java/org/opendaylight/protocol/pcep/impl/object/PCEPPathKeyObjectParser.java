/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeysBuilder;

/**
 * Parser for {@link PathKey}
 */
public class PCEPPathKeyObjectParser extends AbstractEROWithSubobjectsParser {

    public static final int CLASS = 16;

    public static final int TYPE = 1;

    public PCEPPathKeyObjectParser(final EROSubobjectRegistry subReg) {
        super(subReg);
    }

    @Override
    public PathKey parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final PathKeyBuilder builder = new PathKeyBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final List<PathKeys> pk = new ArrayList<>();
        final List<Subobject> subs = parseSubobjects(bytes);
        for (final Subobject s : subs) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase k = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase) s.getSubobjectType();
            pk.add(new PathKeysBuilder().setLoose(s.isLoose()).setPceId(k.getPathKey().getPceId()).setPathKey(k.getPathKey().getPathKey()).build());
        }
        builder.setPathKeys(pk);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof PathKey, "Wrong instance of PCEPObject. Passed %s. Needed PathKeyObject.", object.getClass());
        final PathKey pkey = (PathKey) object;
        final ByteBuf body = Unpooled.buffer();
        final List<PathKeys> pk = pkey.getPathKeys();
        final List<Subobject> subs = new ArrayList<>();
        for (final PathKeys p : pk) {
            subs.add(new SubobjectBuilder().setLoose(p.isLoose()).setSubobjectType(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder().setPathKey(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder().setPathKey(
                                    p.getPathKey()).setPceId(p.getPceId()).build()).build()).build());
        }
        serializeSubobject(subs, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
