/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import java.util.List;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.BidirPimTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.IngressReplicationParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.MldpMp2mpLspParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.MldpP2mpLspParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PimSmTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PimSsmTreeParser;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.RsvpTeP2MpLspParser;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier.SimpleTunnelIdentifierRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Tunnel Identifier PMSI handlers Activator.
 *
 * @author Claudio D. Gasparini
 */
final class TunnelIdentifierActivator {
    private TunnelIdentifierActivator() {
        // Hidden on purpose
    }

    static void registerTunnelIdentifierHandlers(final BGPExtensionProviderContext context,
            final List<Registration> regs) {
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
    }
}
