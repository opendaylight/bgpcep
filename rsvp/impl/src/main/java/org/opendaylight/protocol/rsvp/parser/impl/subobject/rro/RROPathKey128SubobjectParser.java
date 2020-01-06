/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.subobject.rro;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.PathKeyUtils;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class RROPathKey128SubobjectParser implements RROSubobjectParser {
    public static final int TYPE = 65;

    protected static final int PCE128_ID_F_LENGTH = 16;

    private static final int PK_F_LENGTH = 2;
    private static final int PK_F_OFFSET = 0;
    private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;
    private static final int CONTENT128_LENGTH = PCE_ID_F_OFFSET + PCE128_ID_F_LENGTH;

    public static void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        final PathKeyCase pkcase = (PathKeyCase) subobject.getSubobjectType();
        RROSubobjectUtil.formatSubobject(TYPE, PathKeyUtils.serializePathKey(pkcase.getPathKey()), buffer);
    }

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT128_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; "
                + "Expected: >" + CONTENT128_LENGTH + ".");
        }
        return new SubobjectContainerBuilder()
                .setSubobjectType(new PathKeyCaseBuilder()
                    .setPathKey(new PathKeyBuilder()
                        .setPathKey(new PathKey(ByteBufUtils.readUint16(buffer)))
                        .setPceId(new PceId(ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH)))
                        .build())
                    .build())
                .build();
    }
}
