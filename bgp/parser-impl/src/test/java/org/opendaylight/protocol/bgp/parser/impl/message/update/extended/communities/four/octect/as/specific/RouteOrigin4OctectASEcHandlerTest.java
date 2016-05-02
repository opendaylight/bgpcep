/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;

public final class RouteOrigin4OctectASEcHandlerTest {

    static final byte[] INPUT = {
        0, 0, 0, 20, 0, 100
    };
    static final As4SpecificCommon AS_COMMON = new As4SpecificCommonBuilder().setAsNumber(new AsNumber(20L))
        .setLocalAdministrator(100).build();
    RouteOrigin4OctectASEcHandler handler;

    @Before
    public void Setup() {
        this.handler = new RouteOrigin4OctectASEcHandler();
    }

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {

        final As4RouteOriginExtendedCommunityCase expected = new As4RouteOriginExtendedCommunityCaseBuilder()
            .setAs4RouteOriginExtendedCommunity(new As4RouteOriginExtendedCommunityBuilder()
                .setAs4SpecificCommon(AS_COMMON).build()).build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() throws BGPDocumentedException, BGPParsingException {
        this.handler.serializeExtendedCommunity(new As4GenericSpecExtendedCommunityCaseBuilder().build(), null);
    }
}