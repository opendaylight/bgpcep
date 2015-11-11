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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunityBuilder;

public class RouteTargetAsTwoOctetEcHandlerTest {

    private static final byte[] INPUT = {
        0, 35, 4, 2, 8, 7
    };

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {
        final RouteTargetAsTwoOctetEcHandler handler = new RouteTargetAsTwoOctetEcHandler();
        final RouteTargetExtendedCommunityCase expected = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(35L)).setLocalAdministrator(
                        new byte[] { 4, 2, 8, 7 }).build()).build();

        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }

}
