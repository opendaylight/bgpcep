/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;

public class LinkstateNlriParserTest {

    private final byte[] nodeNlri = new byte[] { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x30, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x23,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x29, (byte) 0x29, (byte) 0x29, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x39, (byte) 0x05 };

    private final byte[] linkNlri = new byte[] { (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x53, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x1a,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x18,
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14,
        (byte) 0xa0, (byte) 0x2a, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0xc5, (byte) 0x14, (byte) 0xa0, (byte) 0x28 };

    private final byte[] prefixNlri = new byte[] { (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x39, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02,
        (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00,
        (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x01, (byte) 0x07, (byte) 0x00,
        (byte) 0x02, (byte) 0x00, (byte) 0x0F, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x09,
        (byte) 0x00, (byte) 0x03, (byte) 0x10, (byte) 0xFF, (byte) 0xFF };

    private final byte[] nextHop = new byte[] { (byte) 0x0a, (byte) 0x19, (byte) 0x02, (byte) 0x1b };

    @Test
    public void testNodeNlri() throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser(false);
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.copiedBuffer(this.nodeNlri), this.nextHop, builder);

        assertEquals("10.25.2.27", ((Ipv4NextHopCase) builder.getCNextHop()).getIpv4NextHop().getGlobal().getValue());

        final DestinationLinkstate ls = ((DestinationLinkstateCase) builder.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();

        assertEquals(1, ls.getCLinkstateDestination().size());

        final CLinkstateDestination dest = ls.getCLinkstateDestination().get(0);

        assertEquals(NlriType.Node, dest.getNlriType());
        assertNull(dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(BigInteger.ONE, dest.getIdentifier().getValue());

        final LocalNodeDescriptors nodeD = dest.getLocalNodeDescriptors();
        assertEquals(new AsNumber(72L), nodeD.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), nodeD.getDomainId());
        assertEquals(new IsisPseudonodeCaseBuilder().setIsisPseudonode(
                new IsisPseudonodeBuilder().setPsn((short) 5).setIsIsRouterIdentifier(
                        new IsIsRouterIdentifierBuilder().setIsoSystemId(
                                new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                    (byte) 0x39 })).build()).build()).build(), nodeD.getCRouterIdentifier());

        assertNull(dest.getRemoteNodeDescriptors());

        assertArrayEquals(this.nodeNlri, LinkstateNlriParser.serializeNlri(dest));
    }

    @Test
    public void testLinkNlri() throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser(false);
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.copiedBuffer(this.linkNlri), this.nextHop, builder);

        assertEquals("10.25.2.27", ((Ipv4NextHopCase) builder.getCNextHop()).getIpv4NextHop().getGlobal().getValue());

        final DestinationLinkstate ls = ((DestinationLinkstateCase) builder.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();

        assertEquals(1, ls.getCLinkstateDestination().size());

        final CLinkstateDestination dest = ls.getCLinkstateDestination().get(0);

        assertEquals(NlriType.Link, dest.getNlriType());
        assertNull(dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(BigInteger.ONE, dest.getIdentifier().getValue());

        final LocalNodeDescriptors local = dest.getLocalNodeDescriptors();
        assertEquals(new AsNumber(72L), local.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), local.getDomainId());
        assertEquals(
                new IsisNodeCaseBuilder().setIsisNode(
                        new IsisNodeBuilder().setIsoSystemId(
                                new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                    (byte) 0x42 })).build()).build(), local.getCRouterIdentifier());
        final RemoteNodeDescriptors remote = dest.getRemoteNodeDescriptors();
        assertEquals(new AsNumber(72L), remote.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), remote.getDomainId());
        assertEquals(new OspfNodeCaseBuilder().setOspfNode(new OspfNodeBuilder().setOspfRouterId(0x00000040L).build()).build(),
                remote.getCRouterIdentifier());
        final LinkDescriptors ld = dest.getLinkDescriptors();
        assertEquals("197.20.160.42", ld.getIpv4InterfaceAddress().getValue());
        assertEquals("197.20.160.40", ld.getIpv4NeighborAddress().getValue());

        assertArrayEquals(this.linkNlri, LinkstateNlriParser.serializeNlri(dest));
    }

    @Test
    public void testPrefixNlri() throws BGPParsingException {
        final LinkstateNlriParser parser = new LinkstateNlriParser(false);
        final MpReachNlriBuilder builder = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.copiedBuffer(this.prefixNlri), this.nextHop, builder);

        assertEquals("10.25.2.27", ((Ipv4NextHopCase) builder.getCNextHop()).getIpv4NextHop().getGlobal().getValue());

        final DestinationLinkstate ls = ((DestinationLinkstateCase) builder.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();

        assertEquals(1, ls.getCLinkstateDestination().size());

        final CLinkstateDestination dest = ls.getCLinkstateDestination().get(0);

        assertEquals(NlriType.Ipv4Prefix, dest.getNlriType());
        assertNull(dest.getDistinguisher());
        assertEquals(ProtocolId.IsisLevel2, dest.getProtocolId());
        assertEquals(BigInteger.ONE, dest.getIdentifier().getValue());

        final LocalNodeDescriptors local = dest.getLocalNodeDescriptors();
        assertEquals(new AsNumber(72L), local.getAsNumber());
        assertEquals(new DomainIdentifier(0x28282828L), local.getDomainId());
        assertEquals(
                new IsisNodeCaseBuilder().setIsisNode(
                        new IsisNodeBuilder().setIsoSystemId(
                                new IsoSystemIdentifier(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                    (byte) 0x42 })).build()).build(), local.getCRouterIdentifier());
        assertNull(dest.getRemoteNodeDescriptors());

        final PrefixDescriptors pd = dest.getPrefixDescriptors();
        assertEquals(OspfRouteType.External1, pd.getOspfRouteType());
        assertEquals(new TopologyIdentifier(15), pd.getMultiTopologyId());
        assertEquals(new Ipv4Prefix("255.255.0.0/16"), pd.getIpReachabilityInformation().getIpv4Prefix());

        assertArrayEquals(this.prefixNlri, LinkstateNlriParser.serializeNlri(dest));
    }
}
