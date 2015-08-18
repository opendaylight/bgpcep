/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.subobjects;


import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

public class CommonUnnumberedInterfaceSubobjectParser {
    final protected UnnumberedCase parseUnnumeredInterface(final ByteBuf buffer) {
        final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
        ubuilder.setRouterId(buffer.readUnsignedInt());
        ubuilder.setInterfaceId(buffer.readUnsignedInt());
        return new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build();
    }

    final protected void serializeUnnumeredInterface(final Unnumbered unnumbered, final ByteBuf body) {
        Preconditions.checkArgument(unnumbered.getRouterId() != null, "RouterId is mandatory.");
        writeUnsignedInt(unnumbered.getRouterId(), body);
        Preconditions.checkArgument(unnumbered.getInterfaceId() != null, "InterfaceId is mandatory.");
        writeUnsignedInt(unnumbered.getInterfaceId(), body);
    }
}
