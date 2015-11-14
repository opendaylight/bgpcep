/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;

public final class AsNumberCaseParser {
    private static final int CONTENT_LENGTH = 2;

    private AsNumberCaseParser() {
        throw new UnsupportedOperationException();
    }

    public static AsNumberCase parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                + CONTENT_LENGTH + ".");
        }
        return new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber((long) buffer.readUnsignedShort())).build()).build();
    }


    public static ByteBuf serializeSubobject(final AsNumberCase asCase) {
        final AsNumberSubobject asNumber = asCase.getAsNumber();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        Preconditions.checkArgument(asNumber.getAsNumber() != null, "AsNumber is mandatory.");
        writeShort(asNumber.getAsNumber().getValue().shortValue(), body);
        return body;
    }
}
