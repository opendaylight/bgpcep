/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.rro;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

/**
 * Parser for {@link UnnumberedCase}
 */
public class RROUnnumberedInterfaceSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 4;

    private static final int FLAGS_SIZE = 8;
    private static final int RESERVED = 1;

    private static final int CONTENT_LENGTH = 10;

    private static final int LPA_F_OFFSET = 7;
    private static final int LPIU_F_OFFSET = 6;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                + CONTENT_LENGTH + ".");
        }
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setProtectionAvailable(flags.get(LPA_F_OFFSET));
        builder.setProtectionInUse(flags.get(LPIU_F_OFFSET));
        final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
        buffer.skipBytes(RESERVED);
        ubuilder.setRouterId(buffer.readUnsignedInt());
        ubuilder.setInterfaceId(buffer.readUnsignedInt());
        builder.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof UnnumberedCase, "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final UnnumberedSubobject specObj = ((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered();
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(LPA_F_OFFSET, subobject.isProtectionAvailable());
        flags.set(LPIU_F_OFFSET, subobject.isProtectionInUse());
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        flags.toByteBuf(body);
        body.writeZero(RESERVED);
        Preconditions.checkArgument(specObj.getRouterId() != null, "RouterId is mandatory.");
        writeUnsignedInt(specObj.getRouterId(), body);
        Preconditions.checkArgument(specObj.getInterfaceId() != null, "InterfaceId is mandatory.");
        writeUnsignedInt(specObj.getInterfaceId(), body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}