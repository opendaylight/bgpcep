/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.Ipv4PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.Ipv6PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AdvertisingNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv4NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6InterfaceTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.Ipv6NeighborTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LinkIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.LocalNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.NodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.OspfRouteTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.ReachTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.RemoteNodeDescriptorTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.RouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Activator for registering linkstate extensions to BGP parser.
 */
@MetaInfServices(value = BGPExtensionProviderActivator.class)
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    private static final int LINKSTATE_AFI = 16388;
    private static final int LINKSTATE_SAFI = 71;

    private final boolean ianaLinkstateAttributeType;
    private final RSVPTeObjectRegistry rsvpTeObjectRegistry;

    public BGPActivator() {
        this(true, null);
    }

    // FIXME: this should be properly injected
    public BGPActivator(final boolean ianaLinkstateAttributeType, final RSVPTeObjectRegistry rsvpTeObjectRegistry) {
        this.rsvpTeObjectRegistry = rsvpTeObjectRegistry;
        this.ianaLinkstateAttributeType = ianaLinkstateAttributeType;
    }

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();

        regs.add(context.registerAddressFamily(LinkstateAddressFamily.class, LINKSTATE_AFI));
        regs.add(context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, LINKSTATE_SAFI));

        final NextHopParserSerializer linkstateNextHopParser = new NextHopParserSerializer() {
        };
        final LinkstateNlriParser parser = new LinkstateNlriParser();
        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            parser, linkstateNextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(LinkstateRoutes.class, parser));

        regs.add(context.registerAttributeSerializer(Attributes1.class,
            new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry)));
        final LinkstateAttributeParser linkstateAttributeParser = new LinkstateAttributeParser(
            this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry);
        regs.add(context.registerAttributeParser(linkstateAttributeParser.getType(), linkstateAttributeParser));

        registerNlriCodecs(regs, nlriTypeReg);
        registerNlriTlvCodecs(regs, nlriTypeReg);
        return regs;
    }

    private static void registerNlriCodecs(final List<Registration> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

        final NodeNlriParser nodeParser = new NodeNlriParser();
        regs.add(nlriTypeReg.registerNlriParser(nodeParser.getNlriType(), nodeParser));
        regs.add(nlriTypeReg.registerNlriSerializer(NodeCase.class, nodeParser));

        final LinkNlriParser linkParser = new LinkNlriParser();
        regs.add(nlriTypeReg.registerNlriParser(linkParser.getNlriType(), linkParser));
        regs.add(nlriTypeReg.registerNlriSerializer(LinkCase.class, linkParser));

        final Ipv4PrefixNlriParser ipv4PrefixParser = new Ipv4PrefixNlriParser();
        regs.add(nlriTypeReg.registerNlriParser(ipv4PrefixParser.getNlriType(), ipv4PrefixParser));
        regs.add(nlriTypeReg.registerNlriSerializer(PrefixCase.class, ipv4PrefixParser));
        final Ipv6PrefixNlriParser ipv6PrefixParser = new Ipv6PrefixNlriParser();
        regs.add(nlriTypeReg.registerNlriParser(ipv6PrefixParser.getNlriType(), ipv6PrefixParser));

        final TeLspIpv4NlriParser teLspIpv4Parser = new TeLspIpv4NlriParser();
        regs.add(nlriTypeReg.registerNlriParser(teLspIpv4Parser.getNlriType(), teLspIpv4Parser));
        regs.add(nlriTypeReg.registerNlriSerializer(TeLspCase.class, teLspIpv4Parser));
        final TeLspIpv6NlriParser teLspIpv6Parser = new TeLspIpv6NlriParser();
        regs.add(nlriTypeReg.registerNlriParser(teLspIpv6Parser.getNlriType(), teLspIpv6Parser));
    }

    private static void registerNlriTlvCodecs(final List<Registration> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

        final LocalNodeDescriptorTlvParser localParser = new LocalNodeDescriptorTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(localParser.getType(), localParser));
        regs.add(nlriTypeReg.registerTlvSerializer(localParser.getTlvQName(), localParser));

        final NodeDescriptorTlvParser nodeParser = new NodeDescriptorTlvParser();
        regs.add(nlriTypeReg.registerTlvSerializer(nodeParser.getTlvQName(), nodeParser));

        final AdvertisingNodeDescriptorTlvParser advParser = new AdvertisingNodeDescriptorTlvParser();
        regs.add(nlriTypeReg.registerTlvSerializer(advParser.getTlvQName(), advParser));

        final RemoteNodeDescriptorTlvParser remoteParser = new RemoteNodeDescriptorTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(remoteParser.getType(), remoteParser));
        regs.add(nlriTypeReg.registerTlvSerializer(remoteParser.getTlvQName(), remoteParser));

        final RouterIdTlvParser bgpCrouterIdParser = new RouterIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(bgpCrouterIdParser.getType(), bgpCrouterIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(bgpCrouterIdParser.getTlvQName(), bgpCrouterIdParser));

        final AsNumTlvParser asNumParser = new AsNumTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(asNumParser.getType(), asNumParser));
        regs.add(nlriTypeReg.registerTlvSerializer(asNumParser.getTlvQName(), asNumParser));

        final DomainIdTlvParser bgpDomainIdParser = new DomainIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(bgpDomainIdParser.getType(), bgpDomainIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(bgpDomainIdParser.getTlvQName(), bgpDomainIdParser));

        final AreaIdTlvParser areaIdParser = new AreaIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(areaIdParser.getType(), areaIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(areaIdParser.getTlvQName(), areaIdParser));

        final BgpRouterIdTlvParser bgpRouterIdParser = new BgpRouterIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(bgpRouterIdParser.getType(), bgpRouterIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(bgpRouterIdParser.getTlvQName(), bgpRouterIdParser));

        final MemAsNumTlvParser memAsnParser = new MemAsNumTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(memAsnParser.getType(), memAsnParser));
        regs.add(nlriTypeReg.registerTlvSerializer(memAsnParser.getTlvQName(), memAsnParser));

        final LinkIdTlvParser linkIdParser = new LinkIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(linkIdParser.getType(), linkIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(linkIdParser.getTlvQName(), linkIdParser));

        final Ipv4NeighborTlvParser ipv4nNeighborParser = new Ipv4NeighborTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ipv4nNeighborParser.getType(), ipv4nNeighborParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ipv4nNeighborParser.getTlvQName(), ipv4nNeighborParser));

        final Ipv6NeighborTlvParser ipv6NeighborParser = new Ipv6NeighborTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ipv6NeighborParser.getType(), ipv6NeighborParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ipv6NeighborParser.getTlvQName(), ipv6NeighborParser));

        final Ipv4InterfaceTlvParser ipv4InterfaceParser = new Ipv4InterfaceTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ipv4InterfaceParser.getType(), ipv4InterfaceParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ipv4InterfaceParser.getTlvQName(), ipv4InterfaceParser));

        final Ipv6InterfaceTlvParser ipv6InterfaceParser = new Ipv6InterfaceTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ipv6InterfaceParser.getType(), ipv6InterfaceParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ipv6InterfaceParser.getTlvQName(), ipv6InterfaceParser));

        final MultiTopoIdTlvParser multiTopoIdParser = new MultiTopoIdTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(multiTopoIdParser.getType(), multiTopoIdParser));
        regs.add(nlriTypeReg.registerTlvSerializer(multiTopoIdParser.getTlvQName(), multiTopoIdParser));

        final ReachTlvParser ipv4ReachParser = new ReachTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ipv4ReachParser.getType(), ipv4ReachParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ipv4ReachParser.getTlvQName(), ipv4ReachParser));

        final OspfRouteTlvParser ospfRouterParser = new OspfRouteTlvParser();
        regs.add(nlriTypeReg.registerTlvParser(ospfRouterParser.getType(), ospfRouterParser));
        regs.add(nlriTypeReg.registerTlvSerializer(ospfRouterParser.getTlvQName(), ospfRouterParser));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("ianaLinkstateAttribute", ianaLinkstateAttributeType).toString();
    }
}
