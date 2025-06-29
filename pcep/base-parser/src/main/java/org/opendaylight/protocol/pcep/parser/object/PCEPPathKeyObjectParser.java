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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.key.object.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.key.object.path.key.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.key.object.path.key.PathKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;

/**
 * Parser for {@link PathKey}.
 */
public final class PCEPPathKeyObjectParser extends AbstractEROWithSubobjectsParser {

    private static final int CLASS = 16;
    private static final int TYPE = 1;

    public PCEPPathKeyObjectParser(final EROSubobjectRegistry subReg) {
        super(subReg, CLASS, TYPE);
    }

    @Override
    public PathKey parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final List<PathKeys> pk = new ArrayList<>();
        final List<Subobject> subs = parseSubobjects(bytes);
        for (final Subobject sub : subs) {
            final PathKeyCase pkc = (PathKeyCase) sub.getSubobjectType();
            pk.add(new PathKeysBuilder().setLoose(sub.getLoose()).setPceId(pkc.getPathKey().getPceId())
                .setPathKey(pkc.getPathKey().getPathKey()).build());
        }
        return new PathKeyBuilder()
                .setIgnore(header.getIgnore())
                .setProcessingRule(header.getProcessingRule())
                .setPathKeys(pk)
                .build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof PathKey, "Wrong instance of PCEPObject. Passed %s. Needed PathKeyObject.",
            object.getClass());
        final PathKey pkey = (PathKey) object;
        final ByteBuf body = Unpooled.buffer();
        final List<Subobject> subs = new ArrayList<>();
        for (final PathKeys pk : pkey.nonnullPathKeys()) {
            subs.add(new SubobjectBuilder()
                    .setLoose(pk.getLoose())
                    .setSubobjectType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp
                        .rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder()
                        .setPathKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp
                            .rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder()
                            .setPathKey(pk.getPathKey())
                            .setPceId(pk.getPceId())
                            .build())
                        .build())
                    .build());
        }
        serializeSubobject(subs, body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
