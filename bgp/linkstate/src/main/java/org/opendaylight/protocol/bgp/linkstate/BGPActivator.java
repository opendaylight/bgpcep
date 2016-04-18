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
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkLrIdTlvSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LocalNodeDescriptorTlvC;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfPseudoNodeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.OspfRTypeTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkLrIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
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

        final LocalNodeDescriptorTlvC localParser = new LocalNodeDescriptorTlvC();
        regs.add(nlriTypeReg.registerMessageParser(LocalNodeDescriptorTlvC.LOCAL_NODE_DESCRIPTORS_TYPE, localParser));
        regs.add(nlriTypeReg.registerMessageSerializer(NodeDescriptors.QNAME, localParser));
        regs.add(nlriTypeReg.registerMessageSerializer(LocalNodeDescriptors.QNAME, localParser));
        regs.add(nlriTypeReg.registerMessageSerializer(RemoteNodeDescriptors.QNAME, localParser));
        regs.add(nlriTypeReg.registerMessageSerializer(AdvertisingNodeDescriptors.QNAME, localParser));

        final CrouterIdTlvParser bgpCrouterIdParser = new CrouterIdTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(CrouterIdTlvParser.IGP_ROUTER_ID, bgpCrouterIdParser));
        regs.add(nlriTypeReg.registerMessageSerializer(CRouterIdentifier.QNAME, bgpCrouterIdParser));

        final AsNumTlvParser asNumParser = new AsNumTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(AsNumTlvParser.AS_NUMBER, asNumParser));
        regs.add(nlriTypeReg.registerMessageSerializer(AsNumTlvParser.AS_NUMBER_QNAME, asNumParser));

        final DomainIdTlvParser bgpDomainIdParser = new DomainIdTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(DomainIdTlvParser.BGP_LS_ID, bgpDomainIdParser));
        regs.add(nlriTypeReg.registerMessageSerializer(DomainIdTlvParser.DOMAIN_ID_QNAME, bgpDomainIdParser));

        final AreaIdTlvParser areaIdParser = new AreaIdTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(AreaIdTlvParser.AREA_ID, areaIdParser));
        regs.add(nlriTypeReg.registerMessageSerializer(AreaIdTlvParser.AREA_ID_QNAME, areaIdParser));

        final BgpRouterIdTlvParser bgpRIdParser = new BgpRouterIdTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(BgpRouterIdTlvParser.BGP_ROUTER_ID, bgpRIdParser));
        regs.add(nlriTypeReg.registerMessageSerializer(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME, bgpRIdParser));

        final MemAsNumTlvParser memAsnParser = new MemAsNumTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(MemAsNumTlvParser.MEMBER_AS_NUMBER, memAsnParser));
        regs.add(nlriTypeReg.registerMessageSerializer(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME, memAsnParser));

        final LinkLrIdTlvSerializer linkLrIdParser = new LinkLrIdTlvSerializer();
        regs.add(nlriTypeReg.registerMessageSerializer(LinkLrIdentifiers.QNAME, linkLrIdParser));

        final Ipv6NeighborAddTlvParser ipv6nbouraddParser = new Ipv6NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS, ipv6nbouraddParser));
        regs.add(nlriTypeReg.registerMessageSerializer(Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME, ipv6nbouraddParser));

        final Ipv4IfaceAddTlvParser ipv4ifaddParser = new Ipv4IfaceAddTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS, ipv4ifaddParser));
        regs.add(nlriTypeReg.registerMessageSerializer(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS_QNAME, ipv4ifaddParser));

        final Ipv4NeighborAddTlvParser ipv4nbouraddParser = new Ipv4NeighborAddTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS, ipv4nbouraddParser));
        regs.add(nlriTypeReg.registerMessageSerializer(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME, ipv4nbouraddParser));

        final Ipv6IFaceAddTlvParser ipv6ifaddParser = new Ipv6IFaceAddTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS, ipv6ifaddParser));
        regs.add(nlriTypeReg.registerMessageSerializer(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS_QNAME, ipv6ifaddParser));

        final MultiTopoIdTlvParser multitopoidParser = new MultiTopoIdTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(TlvUtil.MULTI_TOPOLOGY_ID, multitopoidParser));
        regs.add(nlriTypeReg.registerMessageSerializer(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, multitopoidParser));

        final IpReachTlvParser ipreachParser = new IpReachTlvParser();
        regs.add(nlriTypeReg.registerMessageSerializer(IpReachTlvParser.IP_REACHABILITY_QNAME, ipreachParser));

        final OspfRTypeTlvParser ospfrtypeParser = new OspfRTypeTlvParser();
        regs.add(nlriTypeReg.registerMessageParser(OspfRTypeTlvParser.OSPF_ROUTE_TYPE, ospfrtypeParser));
        regs.add(nlriTypeReg.registerMessageSerializer(OspfRTypeTlvParser.OSPF_ROUTE_TYPE_QNAME, ospfrtypeParser));

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
