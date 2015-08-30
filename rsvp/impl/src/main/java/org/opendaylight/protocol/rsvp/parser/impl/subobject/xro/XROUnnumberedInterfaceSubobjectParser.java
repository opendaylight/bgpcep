/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.xro;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.CommonUnnumberedInterfaceSubobjectParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

/**
 * Parser for {@link UnnumberedCase}
 */
public class XROUnnumberedInterfaceSubobjectParser extends CommonUnnumberedInterfaceSubobjectParser implements
    XROSubobjectParser,
    XROSubobjectSerializer {
    public static final int TYPE = 4;

    private static final int RESERVED = 1;

    private static final int CONTENT_LENGTH = 10;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean mandatory) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                + CONTENT_LENGTH + ".");
        }
        buffer.readerIndex(buffer.readerIndex() + RESERVED);
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setMandatory(mandatory);
        builder.setAttribute(ExcludeRouteSubobjects.Attribute.forValue(buffer.readUnsignedByte()));
        builder.setSubobjectType(parseUnnumeredInterface(buffer));
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof UnnumberedCase, "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeZero(RESERVED);
        writeUnsignedByte(subobject.getAttribute() != null ? (short) subobject.getAttribute().getIntValue() : null, body);
        serializeUnnumeredInterface(((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered(), body);
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
