/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.labeled.unicast.next.hop.LabeledUnicastIpv4NextHopParser;
import org.opendaylight.protocol.bgp.labeled.unicast.next.hop.LabeledUnicastIpv6NextHopParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.c.next.hop.LabeledUnicastIpv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.mp.reach.nlri.c.next.hop.LabeledUnicastIpv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int LABELED_UNICAST_SAFI = 4;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>(4);
        final LUNlriParser luNlriParser = new LUNlriParser();

        regs.add(context.registerSubsequentAddressFamily(LabeledUnicastSubsequentAddressFamily.class, LABELED_UNICAST_SAFI));

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, luNlriParser));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, luNlriParser));

        regs.add(context.registerNlriSerializer(LabeledUnicastRoutes.class, luNlriParser));

        final LabeledUnicastIpv4NextHopParser ipv4NextHopParser = new LabeledUnicastIpv4NextHopParser();
        context.registerNextHopParser(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, ipv4NextHopParser);
        context.registerNextHopSerializer(LabeledUnicastIpv4NextHopCase.class, ipv4NextHopParser);

        final LabeledUnicastIpv6NextHopParser ipv6NextHopParser = new LabeledUnicastIpv6NextHopParser();
        context.registerNextHopParser(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, ipv6NextHopParser);
        context.registerNextHopSerializer(LabeledUnicastIpv6NextHopCase.class, ipv6NextHopParser);
        return regs;
    }

}
