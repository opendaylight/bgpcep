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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeySubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

class AbstractPathKeySubobjectParser {

    private final int pceLength;

    private final int contentLength = 2 + this.pceLength;

    AbstractPathKeySubobjectParser(final int pceLength) {
        this.pceLength = pceLength;
    }

    public PathKeySubobject parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != this.contentLength) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                    + this.contentLength + ".");
        }
        final PathKey pathKey = new PathKey(buffer.readUnsignedShort());
        final PceId pceId = new PceId(ByteArray.readBytes(buffer, this.pceLength));
        return new PathKeySubobject() {

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return null;
            }

            @Override
            public PceId getPceId() {
                return pceId;
            }

            @Override
            public PathKey getPathKey() {
                return pathKey;
            }
        };
    }

    public void serializeSubobject(final CSubobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject instanceof PathKeySubobject, "Unknown subobject instance. Passed %s. Needed PathKeySubobject.", subobject.getClass());
        final PathKeySubobject pk = (PathKeySubobject) subobject;
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        body.writeShort(pk.getPathKey().getValue());
        Preconditions.checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk.getPceId().getBinary());
    }
}
