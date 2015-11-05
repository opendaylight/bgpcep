/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.ipv4._case.RouteOriginIpv4Builder;

public class RouteOriginIpv4EcHandlerTest {

    private static final byte[] INPUT = {
        12, 51, 2, 5, 0x15, 0x2d
    };

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {
        final RouteOriginIpv4EcHandler handler = new RouteOriginIpv4EcHandler();
        final RouteOriginIpv4Case expected = new RouteOriginIpv4CaseBuilder().setRouteOriginIpv4(
                new RouteOriginIpv4Builder()
                    .setGlobalAdministrator(new Ipv4Address("12.51.2.5"))
                    .setLocalAdministrator(5421)
                    .build()).build();

        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }

}
