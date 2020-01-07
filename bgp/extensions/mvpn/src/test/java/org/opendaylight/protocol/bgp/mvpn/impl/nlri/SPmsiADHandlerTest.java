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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.multicast.group.opaque.grouping.multicast.group.CGAddressCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.mvpn.choice.SPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.s.pmsi.a.d.grouping.SPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;

public final class SPmsiADHandlerTest {
    private static final byte[] SP_MSI_AD = new byte[]{
        0, 1, 1, 2, 3, 4, 1, 2,
        32, 10, 0, 0, 10,
        32, 12, 0, 0, 12,
        1, 0, 0, 1
    };
    private static final byte[] SP_MSI_AD_LENGTH = new byte[]{
        3, 22,
        0, 1, 1, 2, 3, 4, 1, 2,
        32, 10, 0, 0, 10,
        32, 12, 0, 0, 12,
        1, 0, 0, 1
    };

    private final SPmsiADCase expected = new SPmsiADCaseBuilder()
            .setSPmsiAD(new SPmsiADBuilder()
                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                    .setMulticastSource(new IpAddressNoZone(new Ipv4AddressNoZone("10.0.0.10")))
                    .setMulticastGroup(new CGAddressCaseBuilder()
                            .setCGAddress(new IpAddressNoZone(new Ipv4AddressNoZone("12.0.0.12"))).build())
                    .setOrigRouteIp(new IpAddressNoZone(new Ipv4AddressNoZone("1.0.0.1")))
                    .build()).build();
    private final SPmsiADHandler handler = new SPmsiADHandler();

    @Test
    public void testIntraASIPmsiADParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(SP_MSI_AD)));
    }

    @Test
    public void testIntraASIPmsiADSerializer() {
        assertArrayEquals(SP_MSI_AD_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected)));
    }

    @Test
    public void testGetType() {
        assertEquals(NlriType.SPmsiAD.getIntValue(), this.handler.getType());
    }

    @Test
    public void testGetClazz() {
        assertEquals(SPmsiADCase.class, this.handler.getClazz());
    }
}