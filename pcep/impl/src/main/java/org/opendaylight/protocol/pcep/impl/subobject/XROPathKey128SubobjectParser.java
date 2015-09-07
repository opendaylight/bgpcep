/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.PathKey128Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.PathKey128CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.path.key._128._case.PathKey128;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.path.key._128._case.PathKey128Builder;

/**
 * Parser for {@link PathKey}
 */
public class XROPathKey128SubobjectParser implements XROSubobjectParser {

    public static final int TYPE = 65;

    private static final int PCE128_ID_F_LENGTH = 16;

    private static final int CONTENT128_LENGTH = 2 + PCE128_ID_F_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT128_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                    + CONTENT128_LENGTH + ".");
        }
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH);
        final SubobjectBuilder builder = new SubobjectBuilder();
        final PathKey128Case pk128 = new PathKey128CaseBuilder().setPathKey128(new PathKey128Builder().setPceId(
            new PceId(pceId)).setPathKey(new PathKey(pathKey)).build()).build();
        builder.setMandatory(mandatory);
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(new PathKeyBuilder().setPathKeyChoice(pk128).build()).build());
        return builder.build();
    }

    public static void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
        Preconditions.checkArgument(pk.getPathKeyChoice() instanceof PathKey128Case, "PathKey128 is mandatory.");
        final ByteBuf body = Unpooled.buffer();
        final PathKey128 pk128 = ((PathKey128Case) pk.getPathKeyChoice()).getPathKey128();
        Preconditions.checkArgument(pk128.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk128.getPathKey().getValue(), body);
        Preconditions.checkArgument(pk128.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk128.getPceId().getBinary());
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
