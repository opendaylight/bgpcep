/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class AsNumberCaseParser {
    private static final int CONTENT_LENGTH = 2;

    private AsNumberCaseParser() {
        // Hidden on purpose
    }

    public static AsNumberCase parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; " + "Expected: " + CONTENT_LENGTH + ".");
        }
        return new AsNumberCaseBuilder()
                .setAsNumber(new AsNumberBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(buffer.readUnsignedShort())))
                    .build())
                .build();
    }

    public static ByteBuf serializeSubobject(final AsNumberCase asCase) {
        final AsNumberSubobject asNumber = asCase.getAsNumber();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        checkArgument(asNumber.getAsNumber() != null, "AsNumber is mandatory.");
        body.writeShort(asNumber.getAsNumber().getValue().shortValue());
        return body;
    }
}
