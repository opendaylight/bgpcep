/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.RD;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.RD_MODEL;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValueBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.RD_NID;
import static org.opendaylight.protocol.bgp.evpn.spi.pojo.SimpleEvpnNlriRegistryTest.EVPN_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;

public class EvpnNlriParserTest {
    private static final NodeIdentifier EVPN_CHOICE_NID = new NodeIdentifier(EvpnChoice.QNAME);
    private List<EvpnDestination> dest;
    private EvpnNlriParser parser;

    @Before
    public void setUp() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
        NlriActivator.registerNlriParsers(new ArrayList<>());
        this.dest = Collections.singletonList(new EvpnDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setEvpnChoice(IncMultEthTagRParserTest.createIncMultiCase()).build());
        this.parser = new EvpnNlriParser();
    }

    @Test
    public void testSerializeNlri() throws BGPParsingException {
        final ByteBuf buffer = Unpooled.buffer();
        EvpnNlriParser.serializeNlri(this.dest, buffer);
        assertArrayEquals(IncMultEthTagRParserTest.RESULT, ByteArray.getAllBytes(buffer));

    }

    @Test
    public void testMpUnreach() throws BGPParsingException {
        final MpUnreachNlriBuilder mpReach = new MpUnreachNlriBuilder();
        this.parser.parseNlri(Unpooled.wrappedBuffer(IncMultEthTagRParserTest.RESULT), mpReach);
        assertEquals(createUnreach(), mpReach.build());
    }

    private MpUnreachNlri createUnreach() {
        final MpUnreachNlriBuilder mpReachExpected = new MpUnreachNlriBuilder();
        final WithdrawnRoutes wd = new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
            bgp.evpn.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder().setDestinationEvpn(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.
                destination.type.destination.evpn._case.DestinationEvpnBuilder().setEvpnDestination(this.dest).build()).build()).build();
        return mpReachExpected.setWithdrawnRoutes(wd).build();
    }

    @Test
    public void testMpReach() throws BGPParsingException {
        final MpReachNlriBuilder mpReach = new MpReachNlriBuilder();
        this.parser.parseNlri(Unpooled.wrappedBuffer(IncMultEthTagRParserTest.RESULT), mpReach);

        final MpReachNlriBuilder mpReachExpected = new MpReachNlriBuilder();
        final AdvertizedRoutes wd = new AdvertizedRoutesBuilder().setDestinationType(new DestinationEvpnCaseBuilder().setDestinationEvpn(
            new DestinationEvpnBuilder().setEvpnDestination(this.dest).build()).build()).build();
        mpReachExpected.setAdvertizedRoutes(wd);
        assertEquals(mpReachExpected.build(), mpReach.build());
    }

    @Test
    public void testNullMpReachNlri() throws BGPParsingException {
        final MpReachNlriBuilder mpb = new MpReachNlriBuilder();
        this.parser.parseNlri(Unpooled.buffer(), mpb);
        assertEquals(new MpReachNlriBuilder().build(), mpb.build());
    }

    @Test
    public void testNullMpUnReachNlri() throws BGPParsingException {
        final MpUnreachNlriBuilder mpb = new MpUnreachNlriBuilder();
        this.parser.parseNlri(Unpooled.buffer(), mpb);
        assertEquals(new MpUnreachNlriBuilder().build(), mpb.build());
    }

    @Test
    public void testExtractEvpnDestination() throws BGPParsingException {
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> evpnBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        evpnBI.withNodeIdentifier(EVPN_NID);
        evpnBI.withChild(createMACIpAdvChoice());
        evpnBI.withChild(createValueBuilder(RD_MODEL, RD_NID).build());
        final EvpnDestination destResult = EvpnNlriParser.extractEvpnDestination(evpnBI.build());
        final EvpnDestination expected = new EvpnDestinationBuilder().setRouteDistinguisher(RD).setEvpnChoice(MACIpAdvRParserTest.createdExpectedResult()).build();
        assertEquals(expected, destResult);
    }


    public static ChoiceNode createMACIpAdvChoice() {
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder();
        choice.withNodeIdentifier(EVPN_CHOICE_NID);
        return choice.addChild(MACIpAdvRParserTest.createMacIpCont()).build();
    }

    @Test
    public void testExtractRouteKey() throws BGPParsingException {
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> evpnBI = ImmutableUnkeyedListEntryNodeBuilder.create();
        evpnBI.withNodeIdentifier(EVPN_CHOICE_NID);
        evpnBI.withChild(createValueBuilder(RD_MODEL, RD_NID).build());
        evpnBI.withChild(createMACIpAdvChoice());
        final EvpnDestination destResult = EvpnNlriParser.extractRouteKeyDestination(evpnBI.build());
        final EvpnDestination expected = new EvpnDestinationBuilder().setRouteDistinguisher(RD).setEvpnChoice(MACIpAdvRParserTest.createdExpectedRouteKey()).build();
        assertEquals(expected, destResult);
    }
}