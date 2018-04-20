/*
 *  Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.SourceAsExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.SourceAsExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.source.as.extended.community._case.SourceAsExtendedCommunityBuilder;

public class SourceASHandlerTest {
    private static final byte[] INPUT = {
            0, 1, 0, 0, 0, 0
    };

    private final SourceASHandler handler = new SourceASHandler();

    @Test
    public void testHandler() {
        final SourceAsExtendedCommunityCase expected = new SourceAsExtendedCommunityCaseBuilder()
                .setSourceAsExtendedCommunity(new SourceAsExtendedCommunityBuilder()
                        .setGlobalAdministrator(new ShortAsNumber(1L))
                        .build()).build();

        final ExtendedCommunity exComm = handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        Assert.assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeExtendedCommunity(expected, output);
        Assert.assertArrayEquals(INPUT, output.array());

        assertEquals(9, this.handler.getSubType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlerError() {
        this.handler.serializeExtendedCommunity(new As4GenericSpecExtendedCommunityCaseBuilder().build(),
                null);
    }
}
