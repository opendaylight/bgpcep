/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package parser.impl.subobject.RRO;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import parser.spi.RROSubobjectParser;
import parser.spi.RROSubobjectSerializer;
import parser.spi.RSVPParsingException;

public class RROPathKey32SubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 64;

    private static final int PK_F_LENGTH = 2;
    private static final int PCE_ID_F_LENGTH = 4;

    private static final int PK_F_OFFSET = 0;
    private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

    private static final int CONTENT_LENGTH = PCE_ID_F_OFFSET + PCE_ID_F_LENGTH;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                + CONTENT_LENGTH + ".");
        }
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = ByteArray.readBytes(buffer, PCE_ID_F_LENGTH);
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(pceId));
        pBuilder.setPathKey(new PathKey(pathKey));
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.", subobject.getSubobjectType().getClass());
        final PathKeyCase pkcase = (PathKeyCase) subobject.getSubobjectType();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.path.key._case.PathKey pk = pkcase.getPathKey();
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        Preconditions.checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk.getPceId().getBinary());
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
