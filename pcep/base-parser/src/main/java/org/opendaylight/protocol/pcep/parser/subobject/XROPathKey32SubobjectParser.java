/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link PathKey}.
 */
public class XROPathKey32SubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {
    public static final int TYPE = 64;

    private static final int PCE_ID_F_LENGTH = 4;
    private static final int CONTENT_LENGTH = 2 + PCE_ID_F_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
            + "; Expected: >" + CONTENT_LENGTH + ".");
        }
        final Uint16 pathKey = ByteBufUtils.readUint16(buffer);
        final byte[] pceId = ByteArray.readBytes(buffer, PCE_ID_F_LENGTH);
        final PathKeyBuilder pBuilder = new PathKeyBuilder()
                .setPceId(new PceId(pceId))
                .setPathKey(new PathKey(pathKey));
        final SubobjectBuilder builder = new SubobjectBuilder()
                .setMandatory(mandatory)
                .setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof PathKeyCase,
            "Unknown subobject instance. Passed %s. Needed PathKey.", subobject.getSubobjectType().getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
            .subobjects.subobject.type.path.key._case.PathKey pk =
                ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
        final ByteBuf body = Unpooled.buffer();
        checkArgument(pk.getPceId() != null, "PceId is mandatory.");

        final byte[] pceId = pk.getPceId().getValue();
        if (pceId.length == XROPathKey128SubobjectParser.PCE128_ID_F_LENGTH) {
            XROPathKey128SubobjectParser.serializeSubobject(subobject, buffer);
        }
        checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        checkArgument(pceId.length == PCE_ID_F_LENGTH, "PceId 32 Bit required.");
        body.writeBytes(pceId);
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
