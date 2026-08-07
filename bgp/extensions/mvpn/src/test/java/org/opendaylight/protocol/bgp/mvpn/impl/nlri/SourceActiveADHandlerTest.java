/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceActiveADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceActiveADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.source.active.a.d.grouping.SourceActiveADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;

final class SourceActiveADHandlerTest {
    private static final byte[] SOURCE_ACTIVE = new byte[]{
        0, 1, 1, 2, 3, 4, 1, 2,
        32, 1, 0, 0, 1,
        32, 2, 0, 0, 2,
    };
    private static final byte[] SOURCE_ACTIVE_LENGTH = new byte[]{
        5, 18,
        0, 1, 1, 2, 3, 4, 1, 2,
        32, 1, 0, 0, 1,
        32, 2, 0, 0, 2,
    };

    private final SourceActiveADCase expected = new SourceActiveADCaseBuilder()
            .setSourceActiveAD(new SourceActiveADBuilder()
                    .setRouteDistinguisher(new RouteDistinguisher(new RdIpv4("1.2.3.4:258")))
                    .setMulticastSource(new IpAddressNoZone(new Ipv4AddressNoZone("1.0.0.1")))
                    .setMulticastGroup(new IpAddressNoZone(new Ipv4AddressNoZone("2.0.0.2")))
                    .build())
            .build();
    private final SourceActiveADHandler handler = new SourceActiveADHandler();

    @Test
    void testParser() {
        assertEquals(this.expected, this.handler.parseMvpn(Unpooled.copiedBuffer(SOURCE_ACTIVE)));
    }

    @Test
    void testSerializer() {
        assertArrayEquals(SOURCE_ACTIVE_LENGTH, ByteArray.getAllBytes(this.handler.serializeMvpn(this.expected)));
    }

    @Test
    void testGetType() {
        assertEquals(NlriType.SourceActiveAD, this.handler.getType());
    }

    @Test
    void testGetClazz() {
        assertEquals(SourceActiveADCase.class, this.handler.getClazz());
    }
}
