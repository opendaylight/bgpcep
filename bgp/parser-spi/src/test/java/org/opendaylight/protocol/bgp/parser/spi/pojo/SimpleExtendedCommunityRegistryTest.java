/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.ipv4._case.RouteTargetIpv4Builder;

public class SimpleExtendedCommunityRegistryTest {

    private SimpleExtendedCommunityRegistry register;

    private final ExtendedCommunityParser parser = Mockito.mock(ExtendedCommunityParser.class);
    private final ExtendedCommunitySerializer serializer = Mockito.mock(ExtendedCommunitySerializer.class);

    @Before
    public void setup() throws BGPDocumentedException, BGPParsingException {
        register = new SimpleExtendedCommunityRegistry();
        register.registerExtendedCommunityParser(0, 0, parser);
        register.registerExtendedCommunitySerializer(RouteTargetIpv4Case.class, serializer);
        Mockito.doReturn(0).when(serializer).getType(Mockito.anyBoolean());
        Mockito.doReturn(0).when(serializer).getSubType();
        Mockito.doNothing().when(serializer).serializeExtendedCommunity(Mockito.any(ExtendedCommunity.class), Mockito.any(ByteBuf.class));
        Mockito.doReturn(null).when(parser).parseExtendedCommunity(Mockito.any(ByteBuf.class));

    }

    @Test
    public void testExtendedCommunityRegistry() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf output = Unpooled.buffer();
        register.serializeExtendedCommunity(
                new ExtendedCommunitiesBuilder().setTransitive(true).setExtendedCommunity(new RouteTargetIpv4CaseBuilder().setRouteTargetIpv4(new RouteTargetIpv4Builder().build()).build()).build(), output);
        Mockito.verify(serializer).serializeExtendedCommunity(Mockito.any(ExtendedCommunity.class), Mockito.any(ByteBuf.class));
        //no value serialized, just header
        Assert.assertEquals(2, output.readableBytes());

        final ExtendedCommunities parsedExtendedCommunity = register.parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        Mockito.verify(parser).parseExtendedCommunity(Mockito.any(ByteBuf.class));
        Assert.assertTrue(parsedExtendedCommunity.isTransitive());
        //no value parser
        Assert.assertNull(parsedExtendedCommunity.getExtendedCommunity());
    }

    @Test
    public void testExtendedCommunityRegistryUnknown() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf output = Unpooled.buffer();
        register.serializeExtendedCommunity(
                new ExtendedCommunitiesBuilder().setTransitive(false).setExtendedCommunity(new RouteOriginIpv4CaseBuilder().build()).build(), output);
        //no ex. community was serialized
        Assert.assertEquals(0, output.readableBytes());
        Mockito.verify(serializer, Mockito.never()).serializeExtendedCommunity(Mockito.any(ExtendedCommunity.class), Mockito.any(ByteBuf.class));

        final ExtendedCommunities noExtCommunity = register.parseExtendedCommunity(Unpooled.copiedBuffer(new byte[] {0, 1, 0, 0, 0, 0, 0, 0}));
        //no ext. community was parsed
        Assert.assertNull(noExtCommunity);
        Mockito.verify(parser, Mockito.never()).parseExtendedCommunity(Mockito.any(ByteBuf.class));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRegisterParserOutOfRangeType() {
        register.registerExtendedCommunityParser(1234, 0, parser);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRegisterParserOutOfRangeSubType() {
        register.registerExtendedCommunityParser(0, 1234, parser);
    }

}
