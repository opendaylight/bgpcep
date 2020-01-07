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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.intra.as.i.pmsi.a.d.grouping.IntraAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.IntraAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.IntraAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;

public class IntraAsIPmsiADHandlerTest {
    private static final byte[] INTRA_AS = new byte[]{
        0, 1, 1, 2, 3, 4, 1, 2,
        1, 0, 0, 1
    };
    private static final byte[] INTRA_AS_TYPE_LENGTH = new byte[]{
        1, 12,
        0, 1, 1, 2, 3, 4, 1, 2,
        1, 0, 0, 1
    };

    private final IntraAsIPmsiADCase expected = new IntraAsIPmsiADCaseBuilder()
            .setIntraAsIPmsiAD(new IntraAsIPmsiADBuilder()
                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                    .setOrigRouteIp(new IpAddressNoZone(new Ipv4AddressNoZone("1.0.0.1")))
                    .build()).build();
    private final IntraAsIPmsiADHandler handler = new IntraAsIPmsiADHandler();

    @Test
    public void testIntraASIPmsiADParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(INTRA_AS)));
    }

    @Test
    public void testIntraASIPmsiADSerializer() {
        assertArrayEquals(INTRA_AS_TYPE_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected)));
    }

    @Test
    public void testGetType() {
        assertEquals(NlriType.IntraAsIPmsiAD.getIntValue(), this.handler.getType());
    }

    @Test
    public void testGetClazz() {
        assertEquals(IntraAsIPmsiADCase.class, this.handler.getClazz());
    }
}