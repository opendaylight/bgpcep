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
import org.opendaylight.protocol.bgp.linkstate.nlri.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.CrouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.IpReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.Ipv4IfaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.Ipv4NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.Ipv6IFaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.Ipv6NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.IsisNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.IsisPseudoNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkLrIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.OspfNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.OspfPseudoNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.OspfRTypeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspNlriSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
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

    /*Node Descriptor TLVs*/
    private static final int AS_NUMBER = 512;
    private static final int BGP_LS_ID = 513;
    private static final int AREA_ID = 514;
    private static final int IGP_ROUTER_ID = 515;
    private static final int BGP_ROUTER_ID = 516;
    private static final int MEMBER_AS_NUMBER = 517;

    /*Link Descriptor TLVs*/
    private static final int LINK_LR_IDENTIFIERS = 258;
    private static final int IPV4_IFACE_ADDRESS = 259;
    private static final int IPV4_NEIGHBOR_ADDRESS = 260;
    private static final int IPV6_IFACE_ADDRESS = 261;
    private static final int IPV6_NEIGHBOR_ADDRESS = 262;

    /*Prefix Descriptor TLVs*/
    private static final int OSPF_ROUTE_TYPE = 264;
    private static final int IP_REACHABILITY = 265;

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

        final NodeNlriParser nodeParser = new NodeNlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Node, nodeParser));
        regs.add(nlriTypeReg.registerNlriTypeSerializer(NodeCase.class, nodeParser));

        final LinkNlriParser linkParser = new LinkNlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Link, linkParser));
        regs.add( nlriTypeReg.registerNlriTypeSerializer(LinkCase.class, linkParser));

        final PrefixIpv4NlriParser ipv4PrefParser = new PrefixIpv4NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv4Prefix, ipv4PrefParser));

        final PrefixIpv6NlriParser ipv6PrefParser = new PrefixIpv6NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv6Prefix, ipv6PrefParser));

        final PrefixNlriSerializer prefixSerializer = new PrefixNlriSerializer();
        regs.add(nlriTypeReg.registerNlriTypeSerializer(PrefixCase.class, prefixSerializer));

        final TeLspIpv4NlriParser telSPipv4Parser = new TeLspIpv4NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv4TeLsp, telSPipv4Parser));

        final TeLspIpv6NlriParser telSPipv6Parser = new TeLspIpv6NlriParser();
        regs.add(nlriTypeReg.registerNlriTypeParser(NlriType.Ipv6TeLsp, telSPipv6Parser));

        final TeLspNlriSerializer telSpSerializer = new TeLspNlriSerializer();
        regs.add(nlriTypeReg.registerNlriTypeSerializer(TeLspCase.class, telSpSerializer));

        final AsNumTlvParser asNumParser = new AsNumTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(AS_NUMBER, asNumParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(AS_NUMBER, NlriType.Node, asNumParser));

        final CrouterIdTlvParser bgpCrouterIdParser = new CrouterIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IGP_ROUTER_ID, bgpCrouterIdParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IGP_ROUTER_ID, NlriType.Node, bgpCrouterIdParser));

        final DomainIdTlvParser bgpDomainIdParser = new DomainIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(BGP_LS_ID, bgpDomainIdParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(BGP_LS_ID, NlriType.Node, bgpDomainIdParser));

        final AreaIdTlvParser areaIdParser = new AreaIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(AREA_ID, areaIdParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(AREA_ID, NlriType.Node, areaIdParser));

        final BgpRouterIdTlvParser bgpRIdParser = new BgpRouterIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(BGP_ROUTER_ID, bgpRIdParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(BGP_ROUTER_ID, NlriType.Node, bgpRIdParser));

        final MemAsNumTlvParser memAsnParser = new MemAsNumTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(MEMBER_AS_NUMBER, memAsnParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(MEMBER_AS_NUMBER, NlriType.Node, memAsnParser));

        final LinkLrIdTlvParser linkLrIdParser = new LinkLrIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(LINK_LR_IDENTIFIERS, linkLrIdParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(LINK_LR_IDENTIFIERS, NlriType.Link, linkLrIdParser));

        final Ipv6NeighborAddTlvParser ipv6nbouraddParser = new Ipv6NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IPV6_NEIGHBOR_ADDRESS, ipv6nbouraddParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IPV6_NEIGHBOR_ADDRESS, NlriType.Link, ipv6nbouraddParser));

        final Ipv4IfaceAddTlvParser ipv4ifaddParser = new Ipv4IfaceAddTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IPV4_IFACE_ADDRESS, ipv4ifaddParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IPV4_IFACE_ADDRESS, NlriType.Link, ipv4ifaddParser));

        final Ipv4NeighborAddTlvParser ipv4nbouraddParser = new Ipv4NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IPV4_NEIGHBOR_ADDRESS, ipv4nbouraddParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IPV4_NEIGHBOR_ADDRESS, NlriType.Link, ipv4nbouraddParser));

        final Ipv6IFaceAddTlvParser ipv6ifaddParser = new Ipv6IFaceAddTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IPV6_IFACE_ADDRESS, ipv6ifaddParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IPV6_IFACE_ADDRESS, NlriType.Link, ipv6ifaddParser));

        final MultiTopoIdTlvParser multitopoidParser = new MultiTopoIdTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(TlvUtil.MULTI_TOPOLOGY_ID, multitopoidParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(TlvUtil.MULTI_TOPOLOGY_ID, NlriType.Link, multitopoidParser));

        final IpReachTlvParser ipreachParser = new IpReachTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(IP_REACHABILITY, ipreachParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(IP_REACHABILITY, NlriType.Ipv4Prefix, ipreachParser));

        final OspfRTypeTlvParser ospfrtypeParser = new OspfRTypeTlvParser();
        regs.add(nlriTypeReg.registerNlriTlvParser(OSPF_ROUTE_TYPE, ospfrtypeParser));
        regs.add(nlriTypeReg.registerNlriTlvSerializer(OSPF_ROUTE_TYPE, NlriType.Ipv4Prefix, ospfrtypeParser));

        final IsisNodeCaseSerializer isisNcaseSerializer = new IsisNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(IsisNodeCase.class, isisNcaseSerializer));

        final IsisPseudoNodeCaseSerializer isisPNcaseSerializer = new IsisPseudoNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(IsisPseudonodeCase.class, isisPNcaseSerializer));

        final OspfNodeCaseSerializer ospfNcaseSerializer = new OspfNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(OspfNodeCase.class, ospfNcaseSerializer));

        final OspfPseudoNodeCaseSerializer ospfPNcaseSerializer = new OspfPseudoNodeCaseSerializer();
        regs.add(nlriTypeReg.registerRouterIdSerializer(OspfPseudonodeCase.class, ospfPNcaseSerializer));

        return regs;
    }
}
