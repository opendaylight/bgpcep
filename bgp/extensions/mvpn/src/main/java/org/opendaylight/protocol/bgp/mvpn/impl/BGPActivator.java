/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.PEDistinguisherLabelsAttributeHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.PMSITunnelAttributeHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.BidirPimTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.IngressReplicationParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.MldpMp2mpLspParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.MldpP2mpLspParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PimSmTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PimSsmTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.RsvpTeP2MpLspParser;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.MvpnIpv4NlriHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.MvpnIpv6NlriHandler;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier.SimpleTunnelIdentifierRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.routes.ipv4.MvpnRoutesIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.routes.ipv6.MvpnRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

/**
 * Registers NLRI, Attributes, Extended communities Handlers.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.mvpn.impl.BGPActivator")
@MetaInfServices
public final class BGPActivator implements BGPExtensionProviderActivator {
    @VisibleForTesting
    static final int MVPN_SAFI = 5;

    @Inject
    public BGPActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> start(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        final SimpleTunnelIdentifierRegistry tunnelIdentifierReg = SimpleTunnelIdentifierRegistry.getInstance();

        final RsvpTeP2MpLspParser rsvpTeP2MpLspParser = new RsvpTeP2MpLspParser();
        regs.add(tunnelIdentifierReg.registerParser(rsvpTeP2MpLspParser));
        regs.add(tunnelIdentifierReg.registerSerializer(rsvpTeP2MpLspParser));

        final MldpP2mpLspParser mldpP2mpLspParser = new MldpP2mpLspParser(context.getAddressFamilyRegistry());
        regs.add(tunnelIdentifierReg.registerParser(mldpP2mpLspParser));
        regs.add(tunnelIdentifierReg.registerSerializer(mldpP2mpLspParser));

        final PimSsmTreeParser pimSsmTreeParser = new PimSsmTreeParser();
        regs.add(tunnelIdentifierReg.registerParser(pimSsmTreeParser));
        regs.add(tunnelIdentifierReg.registerSerializer(pimSsmTreeParser));

        final PimSmTreeParser pimSmTreeParser = new PimSmTreeParser();
        regs.add(tunnelIdentifierReg.registerParser(pimSmTreeParser));
        regs.add(tunnelIdentifierReg.registerSerializer(pimSmTreeParser));

        final BidirPimTreeParser bidirPimTreeParser = new BidirPimTreeParser();
        regs.add(tunnelIdentifierReg.registerParser(bidirPimTreeParser));
        regs.add(tunnelIdentifierReg.registerSerializer(bidirPimTreeParser));

        final IngressReplicationParser ingressReplicationParser = new IngressReplicationParser();
        regs.add(tunnelIdentifierReg.registerParser(ingressReplicationParser));
        regs.add(tunnelIdentifierReg.registerSerializer(ingressReplicationParser));

        final MldpMp2mpLspParser mldpMp2mpLspParser = new MldpMp2mpLspParser();
        regs.add(tunnelIdentifierReg.registerParser(mldpMp2mpLspParser));
        regs.add(tunnelIdentifierReg.registerSerializer(mldpMp2mpLspParser));

        regs.add(context.registerSubsequentAddressFamily(McastVpnSubsequentAddressFamily.VALUE, MVPN_SAFI));

        final MvpnIpv4NlriHandler mvpnIpv4NlriHandler = new MvpnIpv4NlriHandler();
        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE,
                mvpnIpv4NlriHandler, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriSerializer(MvpnRoutesIpv4.class, mvpnIpv4NlriHandler));

        final MvpnIpv6NlriHandler mvpnIpv6NlriHandler = new MvpnIpv6NlriHandler();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv6AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE,
                mvpnIpv6NlriHandler, ipv6NextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(MvpnRoutesIpv6.class, mvpnIpv6NlriHandler));

        final PEDistinguisherLabelsAttributeHandler peDistHandler = new PEDistinguisherLabelsAttributeHandler();
        regs.add(context.registerAttributeParser(peDistHandler.getType(), peDistHandler));
        regs.add(context.registerAttributeSerializer(peDistHandler.getClazz(), peDistHandler));

        final PMSITunnelAttributeHandler pmsiParser = new PMSITunnelAttributeHandler();
        regs.add(context.registerAttributeParser(pmsiParser.getType(), pmsiParser));
        regs.add(context.registerAttributeSerializer(pmsiParser.getClazz(), pmsiParser));

        return regs;
    }
}
