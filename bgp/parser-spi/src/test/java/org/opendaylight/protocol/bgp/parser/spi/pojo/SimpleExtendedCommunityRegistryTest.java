/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.ipv4.grouping.RouteTargetIpv4Builder;

@ExtendWith(MockitoExtension.class)
class SimpleExtendedCommunityRegistryTest {
    @Mock
    private ExtendedCommunityParser parser;
    @Mock
    private ExtendedCommunitySerializer serializer;

    private SimpleExtendedCommunityRegistry register;

    @BeforeEach
    void beforeEach() {
        register = new SimpleExtendedCommunityRegistry();
        register.registerExtendedCommunityParser(0, 0, parser);
        register.registerExtendedCommunitySerializer(RouteTargetIpv4Case.class, serializer);
    }

    @Test
    void testExtendedCommunityRegistry() throws Exception {
        doReturn(0).when(serializer).getType(anyBoolean());
        doReturn(0).when(serializer).getSubType();
        doNothing().when(serializer).serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));
        doReturn(null).when(parser).parseExtendedCommunity(any(ByteBuf.class));

        final var output = Unpooled.buffer();
        register.serializeExtendedCommunity(new ExtendedCommunitiesBuilder()
            .setTransitive(true)
            .setExtendedCommunity(new RouteTargetIpv4CaseBuilder()
                .setRouteTargetIpv4(new RouteTargetIpv4Builder().build())
                .build())
            .build(), output);
        verify(serializer).serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));
        //no value serialized, just header
        assertEquals(2, output.readableBytes());

        final var parsedExtendedCommunity =
                register.parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        verify(parser).parseExtendedCommunity(any(ByteBuf.class));
        assertTrue(parsedExtendedCommunity.getTransitive());
        //no value parser
        assertNull(parsedExtendedCommunity.getExtendedCommunity());
    }

    @Test
    void testExtendedCommunityRegistryUnknown() throws Exception {
        final var output = Unpooled.buffer();
        register.serializeExtendedCommunity(new ExtendedCommunitiesBuilder()
            .setTransitive(false)
            .setExtendedCommunity(new RouteOriginIpv4CaseBuilder().build())
            .build(), output);
        //no ex. community was serialized
        assertEquals(0, output.readableBytes());
        verify(serializer, never()).serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));

        final var noExtCommunity =
            register.parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 1, 0, 0, 0, 0, 0, 0}));
        //no ext. community was parsed
        assertNull(noExtCommunity);
        verify(parser, never()).parseExtendedCommunity(any(ByteBuf.class));
    }

    @Test
    public void testRegisterParserOutOfRangeType() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> register.registerExtendedCommunityParser(1234, 0, parser));
        assertEquals("Illegal extended-community type 1234", ex.getMessage());
    }

    @Test
    void testRegisterParserOutOfRangeSubType() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> register.registerExtendedCommunityParser(0, 1234, parser));
        assertEquals("Illegal extended-community sub-type 1234", ex.getMessage());
    }
}
