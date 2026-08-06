/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.EncapsulationTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.EncapsulationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.EncapsulationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.LinkBandwidthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.encapsulation._case.EncapsulationExtendedCommunityBuilder;

class EncapsulationECTest {
    private static final byte[] RESULT = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a };
    private static final int COMMUNITY_VALUE_SIZE = 6;
    private static final EncapsulationTunnelType TUNNEL_TYPE = EncapsulationTunnelType.Mpls;
    private EncapsulationEC parser;

    @BeforeEach
    void setUp() {
        this.parser = new EncapsulationEC();
    }

    @Test
    void testParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf buffer = Unpooled.buffer(COMMUNITY_VALUE_SIZE);

        final EncapsulationCase expected = new EncapsulationCaseBuilder().setEncapsulationExtendedCommunity(
            new EncapsulationExtendedCommunityBuilder().setTunnelType(TUNNEL_TYPE).build()).build();
        this.parser.serializeExtendedCommunity(expected, buffer);
        assertArrayEquals(RESULT, ByteArray.getAllBytes(buffer));

        final ExtendedCommunity result = this.parser.parseExtendedCommunity(Unpooled.wrappedBuffer(RESULT));
        assertEquals(expected, result);
    }

    @Test
    void testWrongCase() {
        assertThrows(IllegalArgumentException.class,
            () -> this.parser.serializeExtendedCommunity(new LinkBandwidthCaseBuilder().build(), null));
    }

    @Test
    void testSubtype() {
        assertEquals(EncapsulationEC.SUBTYPE, this.parser.getSubType());
    }
}
