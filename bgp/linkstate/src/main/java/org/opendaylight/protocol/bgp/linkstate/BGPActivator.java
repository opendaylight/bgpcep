/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.next.hop.LinkstateNextHopParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspNlriSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.CrouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.IpReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4IfaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6IFaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.IsisNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.IsisPseudoNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkLrIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.NodeDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfPseudoNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfRTypeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.PrefixDescTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.RemoteNodeDescTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

/**
 * Activator for registering linkstate extensions to BGP parser.
 */
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int LINKSTATE_AFI = 16388;

    private static final int LINKSTATE_SAFI = 71;

    private final boolean ianaLinkstateAttributeType;

    private final RSVPTeObjectRegistry rsvpTeObjectRegistry;

    public BGPActivator() {
        super();
        this.ianaLinkstateAttributeType = true;
        rsvpTeObjectRegistry = null;
    }

    public BGPActivator(final boolean ianaLinkstateAttributeType, final RSVPTeObjectRegistry rsvpTeObjectRegistry) {
        super();
        this.rsvpTeObjectRegistry = rsvpTeObjectRegistry;
        this.ianaLinkstateAttributeType = ianaLinkstateAttributeType;
    }

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();

        regs.add(context.registerAddressFamily(LinkstateAddressFamily.class, LINKSTATE_AFI));
        regs.add(context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, LINKSTATE_SAFI));

        LinkstateNextHopParser linkstateNextHopParser = new LinkstateNextHopParser();
        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            new LinkstateNlriParser(false), linkstateNextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));

        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
            new LinkstateNlriParser(true), linkstateNextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(LinkstateRoutes.class, new LinkstateNlriParser(false)));

        regs.add(context.registerAttributeSerializer(Attributes1.class, new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry)));
        final LinkstateAttributeParser linkstateAttributeParser = new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry);
        regs.add(context.registerAttributeParser(linkstateAttributeParser.getType(), linkstateAttributeParser));

        registerNlriCodecs(regs, nlriTypeReg);
        registerNlriTlvCodecs(regs, nlriTypeReg);

        return regs;
    }

    private void registerNlriCodecs(final List<AutoCloseable> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

        final NodeNlriParser nodeParser = new NodeNlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Node, nodeParser));
        regs.add(nlriTypeReg.registerNlriTypeSerializer(NodeCase.class, nodeParser));

        final LinkNlriParser linkParser = new LinkNlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Link, linkParser));
        regs.add( nlriTypeReg.registerNlriTypeSerializer(LinkCase.class, linkParser));

        final PrefixNlriParser ipPrefParser = new PrefixNlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv4Prefix, ipPrefParser));
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv6Prefix, ipPrefParser));
        regs.add(nlriTypeReg.registerNlriTypeSerializer(PrefixCase.class, ipPrefParser));

        final TeLspIpv4NlriParser telSPipv4Parser = new TeLspIpv4NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv4TeLsp, telSPipv4Parser));

        final TeLspIpv6NlriParser telSPipv6Parser = new TeLspIpv6NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv6TeLsp, telSPipv6Parser));

        final TeLspNlriSerializer telSpSerializer = new TeLspNlriSerializer();
        regs.add(nlriTypeReg.registerNlriTypeSerializer(TeLspCase.class, telSpSerializer));

    }

    private void registerNlriTlvCodecs(final List<AutoCloseable> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

        final NodeDescTlvParser localNodeTlvParser = new NodeDescTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.NODE_DESCRIPTORS_NID, localNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.NODE_DESCRIPTORS_NID, localNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID, localNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID, localNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID, localNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID, localNodeTlvParser));

        final RemoteNodeDescTlvParser remNodeTlvParser = new RemoteNodeDescTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID, remNodeTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID, remNodeTlvParser));

        final LinkDescTlvParser linkTlvParser = new LinkDescTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.LINK_DESCRIPTORS_NID, linkTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.LINK_DESCRIPTORS_NID, linkTlvParser));

        final PrefixDescTlvParser prefixTlvParser = new PrefixDescTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID, prefixTlvParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID, prefixTlvParser));

        final AsNumTlvParser asNumParser = new AsNumTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(AsNumTlvParser.AS_NUMBER, asNumParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(AsNumTlvParser.AS_NUMBER, asNumParser));

        final CrouterIdTlvParser bgpCrouterIdParser = new CrouterIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(CrouterIdTlvParser.IGP_ROUTER_ID, bgpCrouterIdParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(CrouterIdTlvParser.IGP_ROUTER_ID, bgpCrouterIdParser));

        final DomainIdTlvParser bgpDomainIdParser = new DomainIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(DomainIdTlvParser.BGP_LS_ID, bgpDomainIdParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(DomainIdTlvParser.BGP_LS_ID, bgpDomainIdParser));

        final AreaIdTlvParser areaIdParser = new AreaIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(AreaIdTlvParser.AREA_ID, areaIdParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(AreaIdTlvParser.AREA_ID, areaIdParser));

        final BgpRouterIdTlvParser bgpRIdParser = new BgpRouterIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(BgpRouterIdTlvParser.BGP_ROUTER_ID, bgpRIdParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(BgpRouterIdTlvParser.BGP_ROUTER_ID, bgpRIdParser));

        final MemAsNumTlvParser memAsnParser = new MemAsNumTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(MemAsNumTlvParser.MEMBER_AS_NUMBER, memAsnParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(MemAsNumTlvParser.MEMBER_AS_NUMBER, memAsnParser));

        final LinkLrIdTlvParser linkLrIdParser = new LinkLrIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(LinkLrIdTlvParser.LINK_LR_IDENTIFIERS, linkLrIdParser));

        final Ipv6NeighborAddTlvParser ipv6nbouraddParser = new Ipv6NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS, ipv6nbouraddParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS, ipv6nbouraddParser));

        final Ipv4IfaceAddTlvParser ipv4ifaddParser = new Ipv4IfaceAddTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS, ipv4ifaddParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS, ipv4ifaddParser));

        final Ipv4NeighborAddTlvParser ipv4nbouraddParser = new Ipv4NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS, ipv4nbouraddParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS, ipv4nbouraddParser));

        final Ipv6IFaceAddTlvParser ipv6ifaddParser = new Ipv6IFaceAddTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS, ipv6ifaddParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS, ipv6ifaddParser));

        final MultiTopoIdTlvParser multitopoidParser = new MultiTopoIdTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(TlvUtil.MULTI_TOPOLOGY_ID, multitopoidParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(TlvUtil.MULTI_TOPOLOGY_ID, multitopoidParser));

        final IpReachTlvParser ipreachParser = new IpReachTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(IpReachTlvParser.IP_REACHABILITY, ipreachParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(IpReachTlvParser.IP_REACHABILITY, ipreachParser));

        final OspfRTypeTlvParser ospfrtypeParser = new OspfRTypeTlvParser();
        regs.add(nlriTypeReg.registerNlriSubTlvParser(OspfRTypeTlvParser.OSPF_ROUTE_TYPE, ospfrtypeParser));
        regs.add(nlriTypeReg.registerNlriSubTlvSerializer(OspfRTypeTlvParser.OSPF_ROUTE_TYPE, ospfrtypeParser));

        final IsisNodeCaseSerializer isisNcaseSerializer = new IsisNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(IsisNodeCase.class, isisNcaseSerializer));

        final IsisPseudoNodeCaseSerializer isisPNcaseSerializer = new IsisPseudoNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(IsisPseudonodeCase.class, isisPNcaseSerializer));

        final OspfNodeCaseSerializer ospfNcaseSerializer = new OspfNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(OspfNodeCase.class, ospfNcaseSerializer));

        final OspfPseudoNodeCaseSerializer ospfPNcaseSerializer = new OspfPseudoNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(OspfPseudonodeCase.class, ospfPNcaseSerializer));
    }
}
