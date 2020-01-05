/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

final class AsNumberCaseParser {
    private static final int CONTENT_LENGTH = 2;

    private AsNumberCaseParser() {
        // Hidden on purpose
    }

    static AsNumberCase parseSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                + "; Expected: " + CONTENT_LENGTH + ".");
        }
        return new AsNumberCaseBuilder()
                .setAsNumber(new AsNumberBuilder()
                    .setAsNumber(new AsNumber(ByteBufUtils.readUint16(buffer).toUint32()))
                    .build())
                .build();
    }

    static ByteBuf serializeSubobject(final AsNumberCase asCase) {
        final AsNumberSubobject asNumber = asCase.getAsNumber();
        final ByteBuf body = Unpooled.buffer(CONTENT_LENGTH);
        final AsNumber asn = asNumber.getAsNumber();
        checkArgument(asn != null, "AsNumber is mandatory.");
        body.writeShort(asn.getValue().shortValue());
        return body;
    }
}
