/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.BackupUnnumberedParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.EroMetricParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4BackupEro;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4EroParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4PrefixSidParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6BackupEro;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6EroParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6PrefixSidParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.SIDParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.UnnumberedEroParser;
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
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleBindingSubTlvsRegistry;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.EroMetricCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.SidLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;
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
        this.rsvpTeObjectRegistry = null;
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

        final NextHopParserSerializer linkstateNextHopParser = new NextHopParserSerializer() {
        };
        final LinkstateNlriParser parser = new LinkstateNlriParser();
        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            parser, linkstateNextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(LinkstateRoutes.class, parser));

        regs.add(context.registerAttributeSerializer(Attributes1.class, new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry)));
        final LinkstateAttributeParser linkstateAttributeParser = new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry);
        regs.add(context.registerAttributeParser(linkstateAttributeParser.getType(), linkstateAttributeParser));

        registerNlriCodecs(regs, nlriTypeReg);
        registerNlriTlvCodecs(regs, nlriTypeReg);
        registerBindingSubTlvs(regs);
        return regs;
    }

    private static void registerBindingSubTlvs(final List<AutoCloseable> regs) {
        final SimpleBindingSubTlvsRegistry simpleReg = SimpleBindingSubTlvsRegistry.getInstance();

        final SIDParser sidParser = new SIDParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(sidParser.getType(), sidParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(SidLabelCase.class, sidParser));

        final Ipv4PrefixSidParser prefixSidParser = new Ipv4PrefixSidParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(prefixSidParser.getType(), prefixSidParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(PrefixSidCase.class, prefixSidParser));

        final Ipv6PrefixSidParser ipv6PrefixSidParser = new Ipv6PrefixSidParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(ipv6PrefixSidParser.getType(), ipv6PrefixSidParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(Ipv6PrefixSidCase.class, ipv6PrefixSidParser));

        final EroMetricParser eroMetricParser = new EroMetricParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(eroMetricParser.getType(), eroMetricParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(EroMetricCase.class, eroMetricParser));

        final Ipv4EroParser ipv4EroParser = new Ipv4EroParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(ipv4EroParser.getType(), ipv4EroParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(Ipv4EroCase.class, ipv4EroParser));

        final Ipv4BackupEro ipv4BackupEro = new Ipv4BackupEro();
        regs.add(simpleReg.registerBindingSubTlvsParser(ipv4BackupEro.getType(), ipv4BackupEro));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(Ipv4EroBackupCase.class, ipv4BackupEro));

        final Ipv6EroParser ipv6EroParser = new Ipv6EroParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(ipv6EroParser.getType(), ipv6EroParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(Ipv6EroCase.class, ipv6EroParser));

        final Ipv6BackupEro ipv6BackupEro = new Ipv6BackupEro();
        regs.add(simpleReg.registerBindingSubTlvsParser(ipv6BackupEro.getType(), ipv6BackupEro));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(Ipv6EroBackupCase.class, ipv6BackupEro));

        final UnnumberedEroParser unnumberedEroParser = new UnnumberedEroParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(unnumberedEroParser.getType(), unnumberedEroParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(UnnumberedInterfaceIdEroCase.class, unnumberedEroParser));

        final BackupUnnumberedParser backupUnnumberedParser = new BackupUnnumberedParser();
        regs.add(simpleReg.registerBindingSubTlvsParser(backupUnnumberedParser.getType(), backupUnnumberedParser));
        regs.add(simpleReg.registerBindingSubTlvsSerializer(UnnumberedInterfaceIdBackupEroCase.class, backupUnnumberedParser));
    }

    private static void registerNlriCodecs(final List<AutoCloseable> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

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

    private static void registerNlriTlvCodecs(final List<AutoCloseable> regs, final SimpleNlriTypeRegistry nlriTypeReg) {

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
}
