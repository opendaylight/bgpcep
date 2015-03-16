/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class UnnnumberedSubobjectParser implements SubobjectParser, SubobjectSerializer {

    public static final int TYPE = 4;

    private static final int RESERVED = 1;

    private static final int CONTENT_LENGTH = 10;

    @Override
    public UnnumberedSubobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }
        buffer.skipBytes(RESERVED);
        final long routerId = buffer.readUnsignedInt();
        final long interfaceId = buffer.readUnsignedInt();
        return new UnnumberedSubobject() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public Long getRouterId() {
                return routerId;
            }

            @Override
            public Long getInterfaceId() {
                return interfaceId;
            }
        };
    }

    @Override
    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof UnnumberedSubobject, "Unknown subobject instance. Passed %s. Needed UnnumberedSubobject.", subobject.getClass());
        final UnnumberedSubobject specObj = (UnnumberedSubobject) subobject;
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        body.writeZero(RESERVED);
        Preconditions.checkArgument(specObj.getRouterId() != null, "RouterId is mandatory.");
        body.writeInt(specObj.getRouterId().intValue());
        Preconditions.checkArgument(specObj.getInterfaceId() != null, "InterfaceId is mandatory.");
        body.writeInt(specObj.getInterfaceId().intValue());
    }
}
