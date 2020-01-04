/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link UnnumberedCase}.
 */
public class RROUnnumberedInterfaceSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {
    public static final int TYPE = 4;

    private static final int FLAGS_SIZE = 8;
    private static final int RESERVED = 1;

    private static final int CONTENT_LENGTH = 10;

    private static final int LPA_F_OFFSET = 7;
    private static final int LPIU_F_OFFSET = 6;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: " + CONTENT_LENGTH + ".");
        }
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        buffer.skipBytes(RESERVED);
        final UnnumberedBuilder ubuilder = new UnnumberedBuilder()
                .setRouterId(ByteBufUtils.readUint32(buffer))
                .setInterfaceId(ByteBufUtils.readUint32(buffer));
        final SubobjectBuilder builder = new SubobjectBuilder()
                .setProtectionAvailable(flags.get(LPA_F_OFFSET))
                .setProtectionInUse(flags.get(LPIU_F_OFFSET))
                .setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof UnnumberedCase,
            "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final UnnumberedSubobject specObj = ((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered();
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
        flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        flags.toByteBuf(body);
        body.writeZero(RESERVED);
        checkArgument(specObj.getRouterId() != null, "RouterId is mandatory.");
        writeUnsignedInt(specObj.getRouterId(), body);
        checkArgument(specObj.getInterfaceId() != null, "InterfaceId is mandatory.");
        writeUnsignedInt(specObj.getInterfaceId(), body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
