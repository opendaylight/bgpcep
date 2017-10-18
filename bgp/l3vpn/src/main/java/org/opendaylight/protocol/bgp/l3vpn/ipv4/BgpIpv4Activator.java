/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev171207.l3vpn.ipv4.routes.VpnIpv4Routes;

public final class BgpIpv4Activator extends AbstractBGPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        final VpnIpv4NlriParser nlriParser = new VpnIpv4NlriParser();
        final VpnIpv4NextHopParserSerializer nextHopParser = new VpnIpv4NextHopParserSerializer();

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
            nlriParser, nextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriSerializer(VpnIpv4Routes.class, nlriParser));

        return regs;
    }

}
