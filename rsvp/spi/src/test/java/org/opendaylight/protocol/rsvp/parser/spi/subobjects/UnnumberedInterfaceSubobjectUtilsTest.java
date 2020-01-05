/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class UnnumberedInterfaceSubobjectUtilsTest {
    private final Uint32 routerId = Uint32.valueOf(3735928559L);
    private final Uint32 interfaceId = Uint32.valueOf(3736059631L);
    private final Unnumbered unnumbered1 = new UnnumberedBuilder().setRouterId((Uint32) null).build();
    private final Unnumbered unnumbered2 = new UnnumberedBuilder().setRouterId(Uint32.ONE).setInterfaceId((Uint32) null)
            .build();

    @Test
    public void testProcessing() {
        final ByteBuf input = Unpooled.buffer(8);
        input.writeInt(this.routerId.intValue());
        input.writeInt(this.interfaceId.intValue());
        final UnnumberedCase output = UnnumberedInterfaceSubobjectUtils.parseUnnumeredInterface(input);
        assertEquals(this.routerId, output.getUnnumbered().getRouterId());
        assertEquals(this.interfaceId, output.getUnnumbered().getInterfaceId());

        final ByteBuf bytebuf = Unpooled.buffer(8);
        UnnumberedInterfaceSubobjectUtils.serializeUnnumeredInterface(output.getUnnumbered(), bytebuf);
        assertArrayEquals(input.array(), bytebuf.array());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException1() {
        UnnumberedInterfaceSubobjectUtils.serializeUnnumeredInterface(this.unnumbered1, Unpooled.EMPTY_BUFFER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException2() {
        UnnumberedInterfaceSubobjectUtils.serializeUnnumeredInterface(this.unnumbered2, Unpooled.buffer(4));
    }
}
