/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.ipv4.grouping.RouteTargetIpv4Builder;
import org.opendaylight.yangtools.yang.common.Uint16;

public class RouteTargetIpv4EcHandlerTest {
    private static final byte[] INPUT = {
        12, 51, 2, 5, 0x15, 0x2d
    };

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {
        final RouteTargetIpv4EcHandler handler = new RouteTargetIpv4EcHandler();
        final RouteTargetIpv4Case expected = new RouteTargetIpv4CaseBuilder().setRouteTargetIpv4(
                new RouteTargetIpv4Builder()
                        .setGlobalAdministrator(new Ipv4AddressNoZone("12.51.2.5"))
                        .setLocalAdministrator(Uint16.valueOf(5421))
                        .build()).build();

        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);

        handler.serializeExtendedCommunity(expected, output);
        assertArrayEquals(INPUT, output.array());
    }
}
