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
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriSerializer;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspNlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
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

        final NextHopParserSerializer linkstateNextHopParser = new NextHopParserSerializer(){};
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

        return regs;
    }
}
