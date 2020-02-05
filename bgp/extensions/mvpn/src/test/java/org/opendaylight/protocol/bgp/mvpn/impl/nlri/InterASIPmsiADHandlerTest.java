/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class InterASIPmsiADHandlerTest {

    private static final byte[] INTER_AS = new byte[]{
        0, 1,
        1, 2, 3, 4, 1, 2,
        0, 0, 0, 1
    };
    private static final byte[] INTER_AS_TYPE_LENGTH = new byte[]{
        2, 12,
        0, 1,
        1, 2, 3, 4, 1, 2,
        0, 0, 0, 1
    };

    private final InterAsIPmsiADCase expected = new InterAsIPmsiADCaseBuilder()
            .setInterAsIPmsiAD(new InterAsIPmsiADBuilder()
                    .setSourceAs(new AsNumber(Uint32.ONE))
                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                    .build()
            ).build();
    private final InterASIPmsiADHandler handler = new InterASIPmsiADHandler();


    @Test
    public void testInterASIPmsiADParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(INTER_AS)));
    }

    @Test
    public void testInterASIPmsiADSerializer() {
        assertArrayEquals(INTER_AS_TYPE_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected)));
    }

    @Test
    public void testGetType() {
        assertEquals(NlriType.InterAsIPmsiAD.getIntValue(), this.handler.getType());
    }

    @Test
    public void testGetClazz() {
        assertEquals(InterAsIPmsiADCase.class, this.handler.getClazz());
    }
}