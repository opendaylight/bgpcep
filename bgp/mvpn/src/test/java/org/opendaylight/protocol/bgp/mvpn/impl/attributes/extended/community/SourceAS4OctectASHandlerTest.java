/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.source.as._4.extended.community._case.SourceAs4ExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;

public class SourceAS4OctectASHandlerTest {
    private static final byte[] INPUT = {0, 0, 0, 20, 0, 0};
    private final SourceAS4OctectHandler handler = new SourceAS4OctectHandler();


    @Test
    public void testHandler() {
        final SourceAs4ExtendedCommunityCase expected = new SourceAs4ExtendedCommunityCaseBuilder()
                .setSourceAs4ExtendedCommunity(new SourceAs4ExtendedCommunityBuilder()
                        .setAsNumber(new AsNumber(20L)).build()).build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());

        assertEquals(209, this.handler.getSubType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() {
        this.handler.serializeExtendedCommunity(new As4GenericSpecExtendedCommunityCaseBuilder().build(), null);
    }
}
