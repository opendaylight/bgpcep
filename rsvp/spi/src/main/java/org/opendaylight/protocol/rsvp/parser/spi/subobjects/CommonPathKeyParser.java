/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class CommonPathKeyParser {
    protected CommonPathKeyParser() {

    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
        .subobjects.subobject.type.path.key._case.PathKey parsePathKey(final int pceIdFLength, final ByteBuf buffer) {
        final Uint16 pathKey = ByteBufUtils.readUint16(buffer);
        final byte[] pceId = ByteArray.readBytes(buffer, pceIdFLength);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(pceId));
        pBuilder.setPathKey(new PathKey(pathKey));
        return pBuilder.build();
    }

    public static ByteBuf serializePathKey(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKey pk) {
        final ByteBuf body = Unpooled.buffer();
        checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk.getPceId().getValue());
        return body;
    }
}
