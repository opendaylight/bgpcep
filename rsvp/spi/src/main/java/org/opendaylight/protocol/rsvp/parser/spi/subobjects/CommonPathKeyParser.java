/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;

public class CommonPathKeyParser {
    protected CommonPathKeyParser() {

    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route
        .subobjects.subobject.type.path.key._case.PathKey parsePathKey(final int pceIdFLength, final ByteBuf buffer) {
        final int pathKey = buffer.readUnsignedShort();
        final byte[] pceId = ByteArray.readBytes(buffer, pceIdFLength);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(pceId));
        pBuilder.setPathKey(new PathKey(pathKey));
        return pBuilder.build();
    }

    public static ByteBuf serializePathKey(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKey pk) {
        final ByteBuf body = Unpooled.buffer();
        Preconditions.checkArgument(pk.getPathKey() != null, "PathKey is mandatory.");
        writeUnsignedShort(pk.getPathKey().getValue(), body);
        Preconditions.checkArgument(pk.getPceId() != null, "PceId is mandatory.");
        body.writeBytes(pk.getPceId().getBinary());
        return body;
    }
}
