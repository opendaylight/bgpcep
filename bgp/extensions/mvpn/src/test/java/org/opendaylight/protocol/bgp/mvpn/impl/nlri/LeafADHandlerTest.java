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
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mvpn.impl.NlriActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.inter.as.i.pmsi.a.d.grouping.InterAsIPmsiADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.LeafADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.leaf.a.d.route.key.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.LeafADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.LeafADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.Uint32;

public class LeafADHandlerTest {
    private static final byte[] LEAF_AD = new byte[]{
        2, 12,
        0, 1,
        1, 2, 3, 4, 1, 2,
        0, 0, 0, 1,
        1, 0, 0, 1,
    };
    private static final byte[] LEAF_AD_LENGTH = new byte[]{
        4, 18,
        2, 12,
        0, 1,
        1, 2, 3, 4, 1, 2,
        0, 0, 0, 1,
        1, 0, 0, 1,
    };

    private final LeafADCase expected = new LeafADCaseBuilder().setLeafAD(new LeafADBuilder()
            .setLeafADRouteKey(
                    new InterAsIPmsiADCaseBuilder().setInterAsIPmsiAD(
                            new InterAsIPmsiADBuilder()
                                    .setSourceAs(new AsNumber(Uint32.ONE))
                                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                                    .build()
                    ).build())
            .setOrigRouteIp(new IpAddressNoZone(new Ipv4AddressNoZone("1.0.0.1"))).build())
            .build();
    private final LeafADHandler handler = new LeafADHandler();

    @Before
    public void setUp() {
        NlriActivator.registerNlriParsers(new ArrayList<>());
    }

    @Test
    public void testParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(LEAF_AD)));
    }

    @Test
    public void testSerializer() {
        assertArrayEquals(LEAF_AD_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected)));
    }

    @Test
    public void testGetType() {
        assertEquals(NlriType.LeafAD.getIntValue(), this.handler.getType());
    }

    @Test
    public void testGetClazz() {
        assertEquals(LeafADCase.class, this.handler.getClazz());
    }
}