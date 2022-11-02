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
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createValue;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.NlriModelUtil.RD_NID;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.SimpleEvpnNlriRegistryTest.EVPN_NID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.destination.EvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationEvpnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.evpn._case.DestinationEvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class EvpnNlriParserTest {
    private static final NodeIdentifier EVPN_CHOICE_NID = new NodeIdentifier(EvpnChoice.QNAME);

    private final List<EvpnDestination> dest = List.of(new EvpnDestinationBuilder()
        .setRouteDistinguisher(RD)
        .setEvpnChoice(IncMultEthTagRParserTest.createIncMultiCase()).build());
    private final EvpnNlriParser parser = new EvpnNlriParser();

    private static ChoiceNode createMACIpAdvChoice() {
        return Builders.choiceBuilder()
            .withNodeIdentifier(EVPN_CHOICE_NID)
            .addChild(MACIpAdvRParserTest.createMacIpCont())
            .build();
    }

    @Test
    public void testSerializeNlri() {
        final ByteBuf buffer = Unpooled.buffer();
        EvpnNlriParser.serializeNlri(dest, buffer);
        assertArrayEquals(IncMultEthTagRParserTest.RESULT, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testMpUnreach() throws BGPParsingException {
        final MpUnreachNlriBuilder mpReach = new MpUnreachNlriBuilder()
            .setAfi(L2vpnAddressFamily.VALUE)
            .setSafi(EvpnSubsequentAddressFamily.VALUE);
        parser.parseNlri(Unpooled.wrappedBuffer(IncMultEthTagRParserTest.RESULT), mpReach, null);
        assertEquals(createUnreach(), mpReach.build());
    }

    private MpUnreachNlri createUnreach() {
        final MpUnreachNlriBuilder mpReachExpected = new MpUnreachNlriBuilder()
            .setAfi(L2vpnAddressFamily.VALUE)
            .setSafi(EvpnSubsequentAddressFamily.VALUE);
        final WithdrawnRoutes wd = new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationEvpnCaseBuilder()
                .setDestinationEvpn(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.evpn.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .destination.evpn._case.DestinationEvpnBuilder()
                    .setEvpnDestination(dest).build())
                .build())
            .build();
        return mpReachExpected.setWithdrawnRoutes(wd).build();
    }

    @Test
    public void testMpReach() throws BGPParsingException {
        final MpReachNlriBuilder mpReach = new MpReachNlriBuilder()
            .setAfi(L2vpnAddressFamily.VALUE)
            .setSafi(EvpnSubsequentAddressFamily.VALUE);
        parser.parseNlri(Unpooled.wrappedBuffer(IncMultEthTagRParserTest.RESULT), mpReach, null);

        final MpReachNlriBuilder mpReachExpected = new MpReachNlriBuilder()
            .setAfi(L2vpnAddressFamily.VALUE)
            .setSafi(EvpnSubsequentAddressFamily.VALUE);
        final AdvertizedRoutes wd = new AdvertizedRoutesBuilder().setDestinationType(new DestinationEvpnCaseBuilder()
                .setDestinationEvpn(new DestinationEvpnBuilder().setEvpnDestination(dest).build())
                .build()).build();
        mpReachExpected.setAdvertizedRoutes(wd);
        assertEquals(mpReachExpected.build(), mpReach.build());
    }

    @Test
    public void testNullMpReachNlri() throws BGPParsingException {
        final MpReachNlriBuilder mpb = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.buffer(), mpb, null);
        assertEquals(new MpReachNlriBuilder().build(), mpb.build());
    }

    @Test
    public void testNullMpUnReachNlri() throws BGPParsingException {
        final MpUnreachNlriBuilder mpb = new MpUnreachNlriBuilder();
        parser.parseNlri(Unpooled.buffer(), mpb, null);
        assertEquals(new MpUnreachNlriBuilder().build(), mpb.build());
    }

    @Test
    public void testExtractEvpnDestination() {
        final EvpnDestination destResult = EvpnNlriParser.extractEvpnDestination(Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(EVPN_NID)
            .withChild(createMACIpAdvChoice())
            .withChild(createValue(RD_MODEL, RD_NID))
            .build());
        final EvpnDestination expected = new EvpnDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setEvpnChoice(MACIpAdvRParserTest.createdExpectedResult())
            .build();
        assertEquals(expected, destResult);
    }

    @Test
    public void testExtractRouteKey() {
        final EvpnDestination destResult = EvpnNlriParser.extractRouteKeyDestination(Builders.unkeyedListEntryBuilder()
            .withNodeIdentifier(EVPN_CHOICE_NID)
            .withChild(createValue(RD_MODEL, RD_NID))
            .withChild(createMACIpAdvChoice())
            .build());
        final EvpnDestination expected = new EvpnDestinationBuilder()
            .setRouteDistinguisher(RD)
            .setEvpnChoice(MACIpAdvRParserTest.createdExpectedRouteKey())
            .build();
        assertEquals(expected, destResult);
    }
}
