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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class RROPathKey128SubobjectParser implements RROSubobjectParser {

    public static final int TYPE = 65;

    protected static final int PCE128_ID_F_LENGTH = 16;

    private static final int PK_F_OFFSET = 0;
    private static final int PK_F_LENGTH = 2;
    private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;
    private static final int CONTENT128_LENGTH = PCE_ID_F_OFFSET + PCE128_ID_F_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT128_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                    + "; Expected: >" + CONTENT128_LENGTH + ".");
        }
        return new SubobjectBuilder()
                .setSubobjectType(new PathKeyCaseBuilder()
                    .setPathKey(new PathKeyBuilder()
                        .setPathKey(new PathKey(ByteBufUtils.readUint16(buffer)))
                        .setPceId(new PceId(ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH)))
                        .build())
                    .build())
                .build();
    }

    public static void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final PathKeyCase pkcase = (PathKeyCase) subobject.getSubobjectType();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects
            .subobject.type.path.key._case.PathKey pk = pkcase.getPathKey();
        final ByteBuf body = Unpooled.buffer();
        final PathKey pathKey = pk.getPathKey();
        checkArgument(pathKey != null, "PathKey is mandatory.");
        ByteBufUtils.write(body, pathKey.getValue());
        final PceId pceId = pk.getPceId();
        checkArgument(pceId != null, "PceId is mandatory.");
        body.writeBytes(pceId.getValue());
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
