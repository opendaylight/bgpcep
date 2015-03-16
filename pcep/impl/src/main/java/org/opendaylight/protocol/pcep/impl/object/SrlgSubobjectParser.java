/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SrlgSubobjectParser implements SubobjectParser, SubobjectSerializer {

    public static final int TYPE = 34;

    private static final int CONTENT_LENGTH = 5;

    @Override
    public SrlgSubobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }
        final SrlgId srlg = new SrlgId(buffer.readUnsignedInt());
        return new SrlgSubobject() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public SrlgId getSrlgId() {
                return srlg;
            }
        };
    }

    @Override
    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof SrlgSubobject, "Unknown subobject instance. Passed %s. Needed SrlgCaseSubobject.", subobject.getClass());
        final SrlgSubobject specObj = (SrlgSubobject) subobject;
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        Preconditions.checkArgument(specObj.getSrlgId() != null, "SrlgId is mandatory.");
        body.writeInt(specObj.getSrlgId().getValue().intValue());
    }
}
