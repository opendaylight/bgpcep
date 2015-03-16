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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class AsNumberSubobjectParser implements SubobjectParser, SubobjectSerializer {

    public static final int TYPE = 32;

    public static final int CONTENT_LENGTH = 2;

    @Override
    public AsNumberSubobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }
        final AsNumber as = new AsNumber((long) buffer.readUnsignedShort());
        return new AsNumberSubobject() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public AsNumber getAsNumber() {
                return as;
            }
        };
    }

    @Override
    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof AsNumberSubobject, "Unknown subobject instance. Passed %s. Needed AsNumberSubobject.", subobject.getClass());
        final AsNumberSubobject asNumber = (AsNumberSubobject) subobject;
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        Preconditions.checkArgument(asNumber.getAsNumber() != null, "AsNumber is mandatory.");
        body.writeShort(asNumber.getAsNumber().getValue().intValue());
    }
}
