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
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.PathKey128Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.PathKey32Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.PathKey32CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.path.key._32._case.PathKey32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.subobject.path.key.choice.path.key._32._case.PathKey32Builder;

/**
 * Parser for {@link PathKey}
 */
// TODO: refactor to abstract class
public class EROPathKey32SubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 64;

    private static final int PCE_ID_F_LENGTH = 4;

    private static final int CONTENT_LENGTH = 2 + PCE_ID_F_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                    + CONTENT_LENGTH + ".");
        }
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = ByteArray.readBytes(buffer, PCE_ID_F_LENGTH);
        final SubobjectBuilder builder = new SubobjectBuilder();
        final PathKey32Case pk32 = new PathKey32CaseBuilder().setPathKey32(new PathKey32Builder().setPceId(
            new PceId(pceId)).setPathKey(new PathKey(pathKey)).build()).build();
        builder.setLoose(loose);
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(new PathKeyBuilder().setPathKeyChoice(pk32).build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.", subobject.getSubobjectType().getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
        Preconditions.checkArgument(pk.getPathKeyChoice() != null, "PathKey is mandatory.");
        if (pk.getPathKeyChoice() instanceof PathKey128Case) {
            EROPathKey128SubobjectParser.serializeSubobject(subobject,buffer);
        }
        Preconditions.checkArgument(pk.getPathKeyChoice() instanceof PathKey32Case, "PathKey32 is mandatory.");
        final ByteBuf body = Unpooled.buffer();
        final PathKey32 pk32 = ((PathKey32Case) pk.getPathKeyChoice()).getPathKey32();
        writeUnsignedShort(pk32.getPathKey().getValue(), body);
        Preconditions.checkArgument(pk32.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk32.getPceId().getBinary());
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }
}
