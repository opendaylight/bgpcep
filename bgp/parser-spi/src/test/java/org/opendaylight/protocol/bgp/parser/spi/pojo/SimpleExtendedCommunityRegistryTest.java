/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.ipv4.grouping.RouteTargetIpv4Builder;

public class SimpleExtendedCommunityRegistryTest {

    private SimpleExtendedCommunityRegistry register;

    private final ExtendedCommunityParser parser = mock(ExtendedCommunityParser.class);
    private final ExtendedCommunitySerializer serializer = mock(ExtendedCommunitySerializer.class);

    @Before
    public void setup() throws BGPDocumentedException, BGPParsingException {
        this.register = new SimpleExtendedCommunityRegistry();
        this.register.registerExtendedCommunityParser(0, 0, this.parser);
        this.register.registerExtendedCommunitySerializer(RouteTargetIpv4Case.class, this.serializer);
        doReturn(0).when(this.serializer).getType(anyBoolean());
        doReturn(0).when(this.serializer).getSubType();
        doNothing().when(this.serializer).serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));
        doReturn(null).when(this.parser).parseExtendedCommunity(any(ByteBuf.class));

    }

    @Test
    public void testExtendedCommunityRegistry() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf output = Unpooled.buffer();
        this.register.serializeExtendedCommunity(
                new ExtendedCommunitiesBuilder().setTransitive(true)
                        .setExtendedCommunity(new RouteTargetIpv4CaseBuilder()
                                .setRouteTargetIpv4(new RouteTargetIpv4Builder().build()).build()).build(), output);
        verify(this.serializer).serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));
        //no value serialized, just header
        Assert.assertEquals(2, output.readableBytes());

        final ExtendedCommunities parsedExtendedCommunity =
                this.register.parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        verify(this.parser).parseExtendedCommunity(any(ByteBuf.class));
        Assert.assertTrue(parsedExtendedCommunity.isTransitive());
        //no value parser
        assertNull(parsedExtendedCommunity.getExtendedCommunity());
    }

    @Test
    public void testExtendedCommunityRegistryUnknown() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf output = Unpooled.buffer();
        this.register.serializeExtendedCommunity(
                new ExtendedCommunitiesBuilder().setTransitive(false)
                        .setExtendedCommunity(new RouteOriginIpv4CaseBuilder().build()).build(), output);
        //no ex. community was serialized
        Assert.assertEquals(0, output.readableBytes());
        verify(this.serializer, never())
                .serializeExtendedCommunity(any(ExtendedCommunity.class), any(ByteBuf.class));

        final ExtendedCommunities noExtCommunity = this.register
                .parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 1, 0, 0, 0, 0, 0, 0}));
        //no ext. community was parsed
        assertNull(noExtCommunity);
        verify(this.parser, never()).parseExtendedCommunity(any(ByteBuf.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterParserOutOfRangeType() {
        this.register.registerExtendedCommunityParser(1234, 0, this.parser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterParserOutOfRangeSubType() {
        this.register.registerExtendedCommunityParser(0, 1234, this.parser);
    }
}
