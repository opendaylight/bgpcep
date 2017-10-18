/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.l3vpn.ipv6.routes.VpnIpv6Routes;

/**
 * @author Kevin Wang
 */
public final class BgpIpv6Activator extends AbstractBGPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        final VpnIpv6NlriParser nlriParser = new VpnIpv6NlriParser();
        final VpnIpv6NextHopParserSerializer nextHopParser = new VpnIpv6NextHopParserSerializer();

        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
            nlriParser, nextHopParser, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(VpnIpv6Routes.class, nlriParser));

        return regs;
    }

}
