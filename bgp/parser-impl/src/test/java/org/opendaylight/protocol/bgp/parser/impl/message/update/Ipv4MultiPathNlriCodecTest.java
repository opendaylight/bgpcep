/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;

public class Ipv4MultiPathNlriCodecTest {

    private static final Ipv4Prefix DESTINATION = new Ipv4Prefix("1.1.1.0/24");

    byte[] MP_NLRI_BYTES = new byte[] {
        0x0, 0x0, 0x0, 0x1, 0x18, 0x1, 0x1, 0x1,
        0x0, 0x0, 0x0, 0x2, 0x18, 0x1, 0x1, 0x1};

    @Test
    public void testParseMpUnreachNlri() throws BGPParsingException {
        final Ipv4MultiPathNlriParser parser = new Ipv4MultiPathNlriParser();
        final MpUnreachNlri mpUnreachNlri = new MpUnreachNlriBuilder().setWithdrawnRoutes(
                new WithdrawnRoutesBuilder().setDestinationType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(
                                        Lists.newArrayList(
                                                createIpv4Prefix(1L, DESTINATION),
                                                createIpv4Prefix(2L, DESTINATION))).build()).build()).build()).build();
        final MpUnreachNlriBuilder mpUnreachNlriBuilder = new MpUnreachNlriBuilder();
        parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpUnreachNlriBuilder);
        Assert.assertEquals(mpUnreachNlri, mpUnreachNlriBuilder.build());

        final WithdrawnRoutesSerializer serializer = new WithdrawnRoutesSerializer();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder().addAugmentation(Attributes2.class,
                new Attributes2Builder().setMpUnreachNlri(mpUnreachNlri).build()).build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }

    @Test
    public void testParseMpReachNlri() throws BGPParsingException {
        final Ipv4MultiPathNlriParser parser = new Ipv4MultiPathNlriParser();
        final MpReachNlri mpReachNlri = new MpReachNlriBuilder().setAdvertizedRoutes(
                new AdvertizedRoutesBuilder().setDestinationType(
                        new DestinationIpv4CaseBuilder().setDestinationIpv4(
                                new DestinationIpv4Builder().setIpv4Prefixes(
                                        Lists.newArrayList(
                                                createIpv4Prefix(1L, DESTINATION),
                                                createIpv4Prefix(2L, DESTINATION))).build()).build()).build()).build();
        final MpReachNlriBuilder mpReachNlriBuilder = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.wrappedBuffer(MP_NLRI_BYTES), mpReachNlriBuilder);
        Assert.assertEquals(mpReachNlri, mpReachNlriBuilder.build());

        final AdvertizedRoutesSerializer serializer = new AdvertizedRoutesSerializer();
        final ByteBuf output = Unpooled.buffer(MP_NLRI_BYTES.length);
        final Attributes attributes = new AttributesBuilder().addAugmentation(Attributes1.class,
                new Attributes1Builder().setMpReachNlri(mpReachNlri).build()).build();
        serializer.serializeAttribute(attributes, output);
        Assert.assertArrayEquals(MP_NLRI_BYTES, output.array());
    }

    private static Ipv4Prefixes createIpv4Prefix(final long pathId, final Ipv4Prefix prefix) {
        return new Ipv4PrefixesBuilder().setPathId(new PathId(pathId)).setPrefix(prefix).build();
    }

}
