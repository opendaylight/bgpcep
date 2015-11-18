/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.spi;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;

/**
 * Parser for {@link PathKey}
 */
public abstract class AbstractEROPathKeySubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE_32 = 64;
    public static final int TYPE_128 = 65;
    protected static final int PCE_ID_F_LENGTH = 4;
    protected static final int PCE128_ID_F_LENGTH = 16;
    protected static final int CONTENT128_LENGTH = 2 + PCE128_ID_F_LENGTH;
    protected static final int CONTENT_LENGTH = 2 + PCE_ID_F_LENGTH;

    protected abstract byte[] readPceId(final ByteBuf buffer);

    protected abstract void checkContentLenght(final int i) throws PCEPDeserializerException;

    @Override
    public final Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        checkContentLenght(buffer.readableBytes());
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = readPceId(buffer);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(pceId));
        pBuilder.setPathKey(new PathKey(pathKey));
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(loose);
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        return builder.build();
    }

    @Override
    public final void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.",
            subobject.getSubobjectType().getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
            .subobjects.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
        Preconditions.checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        Preconditions.checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        final byte[] pceID = pk.getPceId().getBinary();
        Preconditions.checkArgument(pceID.length == PCE_ID_F_LENGTH || pceID.length == PCE128_ID_F_LENGTH, "PceId 32/128 Bit required.");
        final ByteBuf body = Unpooled.buffer();
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        body.writeBytes(pceID);
        EROSubobjectUtil.formatSubobject(pceID.length == PCE_ID_F_LENGTH ? TYPE_32 : TYPE_128, subobject.isLoose(), body, buffer);
    }
}
