/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.route.target;

import static org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOrigin4OctectASEcHandlerTest.AS_COMMON;
import static org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOrigin4OctectASEcHandlerTest.INPUT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.as._4.route.target.extended.community.grouping.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteTargetExtendedCommunityCaseBuilder;

public class RouteTarget4OctectASEcHandlerTest {
    private final RouteTarget4OctectASEcHandler handler = new RouteTarget4OctectASEcHandler();

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {
        final As4RouteTargetExtendedCommunityCase expected = new As4RouteTargetExtendedCommunityCaseBuilder()
                .setAs4RouteTargetExtendedCommunity(new As4RouteTargetExtendedCommunityBuilder()
                        .setAs4SpecificCommon(AS_COMMON).build()).build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() throws BGPDocumentedException, BGPParsingException {
        this.handler.serializeExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder().build(), null);
    }
}