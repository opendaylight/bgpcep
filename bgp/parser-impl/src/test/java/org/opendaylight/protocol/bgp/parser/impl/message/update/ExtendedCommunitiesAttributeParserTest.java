/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;

public class ExtendedCommunitiesAttributeParserTest {

    private static final byte[] INPUT = {
        0x40, 0x03, 0x00, 54, 0, 0, 1, 76
    };

    private static final byte[] UNKOWN = {
        0x40, 0x05, 0x00, 54, 0, 0, 1, 76
    };

    private ExtendedCommunitiesAttributeParser handler;

    @Before
    public void setUp() {
        final ExtendedCommunityRegistry exReg = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getExtendedCommunityReistry();
        handler = new ExtendedCommunitiesAttributeParser(exReg);
    }

    @Test
    public void testExtendedCommunityAttributeParser() throws BGPDocumentedException, BGPParsingException {
        final RouteOriginExtendedCommunityCase routeOrigin = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber(54L)).setLocalAdministrator(
                        new byte[] { 0, 0, 1, 76 }).build()).build();
        final ExtendedCommunities expected = new ExtendedCommunitiesBuilder().setTransitive(false).setExtendedCommunity(routeOrigin).build();
        final AttributesBuilder attBuilder = new AttributesBuilder();

        handler.parseAttribute(Unpooled.copiedBuffer(INPUT), attBuilder);
        final ExtendedCommunities parsed = attBuilder.getExtendedCommunities().get(0);
        Assert.assertEquals(expected, parsed);

        final ByteBuf output = Unpooled.buffer(INPUT.length);
        handler.serializeAttribute(attBuilder.build(), output);
        Assert.assertArrayEquals(Bytes.concat(new byte[] {(byte)192, 16, 8}, INPUT), ByteArray.readAllBytes(output));
    }

    @Test
    public void testExtendedCommunityAttributeParserUnknown() throws BGPDocumentedException, BGPParsingException {
        final AttributesBuilder attBuilder = new AttributesBuilder();
        handler.parseAttribute(Unpooled.copiedBuffer(UNKOWN), attBuilder);
        Assert.assertTrue(attBuilder.getExtendedCommunities().isEmpty());
    }

}
