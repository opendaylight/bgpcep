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

public class CUISubobjectParserTest {
    private final Long routerId = 3735928559L;
    private final Long interfaceId = 3736059631L;
    CommonUnnumberedInterfaceSubobjectParser parser = new CommonUnnumberedInterfaceSubobjectParser();
    final Unnumbered unnumbered1 = new UnnumberedBuilder().setRouterId(null).build();
    final Unnumbered unnumbered2 = new UnnumberedBuilder().setRouterId(1L).setInterfaceId(null).build();

    @Test
    public void testProcessing() {
        final ByteBuf input = Unpooled.buffer(8);
        input.writeInt(this.routerId.intValue());
        input.writeInt(this.interfaceId.intValue());
        final UnnumberedCase output = this.parser.parseUnnumeredInterface(input);
        assertEquals(this.routerId, output.getUnnumbered().getRouterId());
        assertEquals(this.interfaceId, output.getUnnumbered().getInterfaceId());

        final ByteBuf bytebuf = Unpooled.buffer(8);
        this.parser.serializeUnnumeredInterface(output.getUnnumbered(), bytebuf);
        assertArrayEquals(input.array(), bytebuf.array());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testException1() {
        this.parser.serializeUnnumeredInterface(this.unnumbered1, Unpooled.EMPTY_BUFFER);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testException2() {
        this.parser.serializeUnnumeredInterface(this.unnumbered2, Unpooled.buffer(4));
    }

}
