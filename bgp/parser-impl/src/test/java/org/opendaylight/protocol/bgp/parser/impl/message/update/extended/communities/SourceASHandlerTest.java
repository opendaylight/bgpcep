/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.SourceAsExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.SourceAsExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.source.as.extended.community._case.SourceAsExtendedCommunityBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

class SourceASHandlerTest {
    private static final byte[] INPUT = {0, 1, 0, 0, 0, 0};

    private final SourceASHandler handler = new SourceASHandler();

    @Test
    void testHandler() {
        final SourceAsExtendedCommunityCase expected = new SourceAsExtendedCommunityCaseBuilder()
                .setSourceAsExtendedCommunity(new SourceAsExtendedCommunityBuilder()
                        .setGlobalAdministrator(new ShortAsNumber(Uint32.ONE))
                        .build()).build();

        final ExtendedCommunity exComm = this.handler.parseExtendedCommunity(Unpooled.copiedBuffer(INPUT));
        assertEquals(expected, exComm);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        this.handler.serializeExtendedCommunity(expected, output);
        assertArrayEquals(INPUT, output.array());

        assertEquals(9, this.handler.getSubType());
    }

    @Test
    void testHandlerError() {
        assertThrows(IllegalArgumentException.class, () -> this.handler.serializeExtendedCommunity(
            new As4GenericSpecExtendedCommunityCaseBuilder().build(), null));
    }
}
