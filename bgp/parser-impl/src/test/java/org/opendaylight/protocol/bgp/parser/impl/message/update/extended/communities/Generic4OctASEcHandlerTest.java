/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOrigin4OctectASEcHandlerTest.AS_COMMON;
import static org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOrigin4OctectASEcHandlerTest.INPUT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific.Generic4OctASEcHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4GenericSpecExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.as._4.generic.spec.extended.community._case.As4GenericSpecExtendedCommunityBuilder;

public final class Generic4OctASEcHandlerTest {
    Generic4OctASEcHandler handler;

    @Before
    public void before() {
        this.handler = new Generic4OctASEcHandler();
    }

    @Test
    public void testHandler() throws BGPDocumentedException, BGPParsingException {
        final As4GenericSpecExtendedCommunityCase expected = new As4GenericSpecExtendedCommunityCaseBuilder()
            .setAs4GenericSpecExtendedCommunity(new As4GenericSpecExtendedCommunityBuilder()
                .setAs4SpecificCommon(AS_COMMON).build()).build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() {
        this.handler.serializeExtendedCommunity(new As4RouteOriginExtendedCommunityCaseBuilder().build(), null);
    }
}