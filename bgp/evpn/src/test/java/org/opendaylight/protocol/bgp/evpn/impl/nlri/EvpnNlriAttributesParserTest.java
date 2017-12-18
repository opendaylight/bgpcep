/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static java.util.Collections.singletonList;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.RD;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;

public class EvpnNlriAttributesParserTest {
    private EvpnNlriParser parser;

    @Before
    public void setUp() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
        NlriActivator.registerNlriParsers(new ArrayList<>());
        this.parser = new EvpnNlriParser();
    }

    @Test
    public void testAttributes1() throws BGPParsingException {
        final ByteBuf buffer = Unpooled.buffer();
        final Attributes att = new AttributesBuilder().addAugmentation(Attributes1.class,
            new Attributes1Builder().setMpReachNlri(createReach()).build()).build();
        this.parser.serializeAttribute(att, buffer);
        Assert.assertArrayEquals(IncMultEthTagRParserTest.RESULT, ByteArray.getAllBytes(buffer));
    }

    private static MpReachNlri createReach() {
        final MpReachNlriBuilder mpReachExpected = new MpReachNlriBuilder();
        final AdvertizedRoutes wd = new AdvertizedRoutesBuilder().setDestinationType(new DestinationEvpnCaseBuilder()
            .setDestinationEvpn(new DestinationEvpnBuilder().setEvpnDestination(
                    singletonList(new EvpnDestinationBuilder()
                            .setRouteDistinguisher(RD)
                            .setEvpnChoice(IncMultEthTagRParserTest.createIncMultiCase())
                            .build())).build()).build()).build();
        return mpReachExpected.setAdvertizedRoutes(wd).build();
    }

    @Test
    public void testAttributes2() throws BGPParsingException {
        final ByteBuf buffer = Unpooled.buffer();
        final Attributes att = new AttributesBuilder().addAugmentation(Attributes2.class,
            new Attributes2Builder().setMpUnreachNlri(createUnreach()).build()).build();
        this.parser.serializeAttribute(att, buffer);
        Assert.assertArrayEquals(IncMultEthTagRParserTest.RESULT, ByteArray.getAllBytes(buffer));
    }

    private static MpUnreachNlri createUnreach() {
        final MpUnreachNlriBuilder mpReachExpected = new MpUnreachNlriBuilder();
        final WithdrawnRoutes wd = new WithdrawnRoutesBuilder()
                .setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn
                        .rev171213.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationEvpnCaseBuilder().setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn
                        .opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.update.attributes.mp.unreach.nlri.withdrawn
                        .routes.destination.type.destination.evpn._case.DestinationEvpnBuilder()
                        .setEvpnDestination(singletonList(new EvpnDestinationBuilder()
                        .setRouteDistinguisher(RD).setEvpnChoice(IncMultEthTagRParserTest.createIncMultiCase())
                                .build())).build()).build()).build();
        return mpReachExpected.setWithdrawnRoutes(wd).build();
    }

}
