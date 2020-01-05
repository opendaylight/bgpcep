/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link PathKey}.
 */
public abstract class AbstractEROPathKeySubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {
    public static final int TYPE_32 = 64;
    public static final int TYPE_128 = 65;

    protected static final int PCE_ID_F_LENGTH = 4;
    protected static final int PCE128_ID_F_LENGTH = 16;
    protected static final int CONTENT128_LENGTH = 2 + PCE128_ID_F_LENGTH;
    protected static final int CONTENT_LENGTH = 2 + PCE_ID_F_LENGTH;

    protected abstract byte[] readPceId(ByteBuf buffer);

    protected abstract void checkContentLength(int length) throws PCEPDeserializerException;

    @Override
    public final Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        checkContentLength(buffer.readableBytes());
        return new SubobjectBuilder()
                .setLoose(loose)
                .setSubobjectType(new PathKeyCaseBuilder()
                    .setPathKey(new PathKeyBuilder()
                        .setPathKey(new PathKey(ByteBufUtils.readUint16(buffer)))
                        .setPceId(new PceId(readPceId(buffer)))
                        .build())
                    .build())
                .build();
    }

    @Override
    public final void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final SubobjectType type = subobject.getSubobjectType();
        checkArgument(type instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.",
            type.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
            .subobjects.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) type).getPathKey();

        final PathKey pathKey = pk.getPathKey();
        checkArgument(pathKey != null, "PathKey is mandatory.");
        final byte[] pceID = pk.getPceId().getValue();
        checkArgument(pceID.length == PCE_ID_F_LENGTH || pceID.length == PCE128_ID_F_LENGTH,
                "PceId 32/128 Bit required.");
        final ByteBuf body = Unpooled.buffer();
        ByteBufUtils.write(body, pathKey.getValue());
        body.writeBytes(pceID);
        EROSubobjectUtil.formatSubobject(pceID.length == PCE_ID_F_LENGTH ? TYPE_32 : TYPE_128, subobject.isLoose(),
                body, buffer);
    }
}
