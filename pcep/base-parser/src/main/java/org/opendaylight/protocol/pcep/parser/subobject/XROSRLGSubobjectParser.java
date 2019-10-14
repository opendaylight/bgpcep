/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.util.ByteBufUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.SrlgCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.SrlgCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.srlg._case.SrlgBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Parser for {@link SrlgCase}
 */
public class XROSRLGSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 34;

    private static final int CONTENT_LENGTH = 6;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: " + CONTENT_LENGTH + ".");
        }
        final SubobjectBuilder builder = new SubobjectBuilder()
                .setMandatory(mandatory)
                .setSubobjectType(new SrlgCaseBuilder()
                    .setSrlg(new SrlgBuilder()
                        .setSrlgId(new SrlgId(ByteBufUtils.readUint32(buffer)))
                        .build())
                    .build());
        buffer.readByte();
        return builder.setAttribute(ExcludeRouteSubobjects.Attribute.forValue(buffer.readUnsignedByte()))
                .build();
    }

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        checkArgument(subobject.getSubobjectType() instanceof SrlgCase,
            "Unknown subobject instance. Passed %s. Needed SrlgCase.", subobject.getSubobjectType().getClass());
        final SrlgSubobject specObj = ((SrlgCase) subobject.getSubobjectType()).getSrlg();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        checkArgument(specObj.getSrlgId() != null, "SrlgId is mandatory.");
        writeUnsignedInt(specObj.getSrlgId().getValue(), body);
        checkArgument(subobject.getAttribute() != null, "Attribute is mandatory.");
        writeUnsignedByte((Uint8) null, body);
        writeUnsignedByte((short) subobject.getAttribute().getIntValue(), body);
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
