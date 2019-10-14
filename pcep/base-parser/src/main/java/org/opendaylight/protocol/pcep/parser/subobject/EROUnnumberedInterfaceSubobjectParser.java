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
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteBufUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

/**
 * Parser for {@link UnnumberedCase}
 */
public class EROUnnumberedInterfaceSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 4;

    private static final int RESERVED = 2;

    private static final int CONTENT_LENGTH = 10;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: " + CONTENT_LENGTH + ".");
        }
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(loose);
        final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
        buffer.skipBytes(RESERVED);
        ubuilder.setRouterId(ByteBufUtils.readUint32(buffer));
        ubuilder.setInterfaceId(ByteBufUtils.readUint32(buffer));
        builder.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof UnnumberedCase,
            "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final UnnumberedSubobject specObj = ((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeZero(RESERVED);
        checkArgument(specObj.getRouterId() != null, "RouterId is mandatory.");
        writeUnsignedInt(specObj.getRouterId(), body);
        checkArgument(specObj.getInterfaceId() != null, "InterfaceId is mandatory");
        writeUnsignedInt(specObj.getInterfaceId(), body);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }
}
