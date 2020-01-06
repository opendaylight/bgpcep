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
import org.opendaylight.bgpcep.rsvp.util.XROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
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
public class XROPathKey128SubobjectParser implements XROSubobjectParser {
    public static final int TYPE = 65;

    protected static final int PCE128_ID_F_LENGTH = 16;

    private static final int CONTENT128_LENGTH = 2 + PCE128_ID_F_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT128_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: >" + CONTENT128_LENGTH + ".");
        }
        return new SubobjectBuilder()
                .setMandatory(mandatory)
                .setSubobjectType(new PathKeyCaseBuilder()
                    .setPathKey(new PathKeyBuilder()
                        .setPathKey(new PathKey(ByteBufUtils.readUint16(buffer)))
                        .setPceId(new PceId(ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH)))
                        .build())
                    .build())
                .build();
    }

    public static void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final SubobjectType type = subobject.getSubobjectType();
        checkArgument(type instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.",
            type.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
            .subobjects.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) type).getPathKey();
        final ByteBuf body = Unpooled.buffer();
        final PathKey pathKey = pk.getPathKey();
        checkArgument(pathKey != null, "PathKey is mandatory.");
        ByteBufUtils.write(body, pathKey.getValue());
        final PceId pceId = pk.getPceId();
        checkArgument(pceId != null, "PceId is mandatory.");
        body.writeBytes(pceId.getValue());
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
