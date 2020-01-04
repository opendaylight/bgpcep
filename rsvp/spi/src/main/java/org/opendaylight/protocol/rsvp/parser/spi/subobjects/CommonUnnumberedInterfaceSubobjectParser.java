/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class CommonUnnumberedInterfaceSubobjectParser {
    protected CommonUnnumberedInterfaceSubobjectParser() {

    }

    protected static UnnumberedCase parseUnnumeredInterface(final ByteBuf buffer) {
        return new UnnumberedCaseBuilder()
                .setUnnumbered(new UnnumberedBuilder()
                    .setRouterId(ByteBufUtils.readUint32(buffer))
                    .setInterfaceId(ByteBufUtils.readUint32(buffer))
                    .build())
                .build();
    }

    protected static void serializeUnnumeredInterface(final Unnumbered unnumbered, final ByteBuf body) {
        checkArgument(unnumbered.getRouterId() != null, "RouterId is mandatory.");
        ByteBufWriteUtil.writeUnsignedInt(unnumbered.getRouterId(), body);
        checkArgument(unnumbered.getInterfaceId() != null, "InterfaceId is mandatory.");
        ByteBufWriteUtil.writeUnsignedInt(unnumbered.getInterfaceId(), body);
    }
}
