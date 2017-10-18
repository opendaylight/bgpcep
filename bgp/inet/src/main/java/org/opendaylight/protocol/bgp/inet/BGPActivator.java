/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.inet.codec.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6BgpPrefixSidParser;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6NlriParser;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171122.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>(6);

        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        final Ipv4NlriParser ipv4Codec = new Ipv4NlriParser();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
                ipv4Codec, ipv4NextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(Ipv4Routes.class, ipv4Codec));

        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        final Ipv6NlriParser ipv6Codec = new Ipv6NlriParser();
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, ipv6Codec,
                ipv6NextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(Ipv6Routes.class, ipv6Codec));

        final Ipv6BgpPrefixSidParser tlvHandler = new Ipv6BgpPrefixSidParser();
        regs.add(context.registerBgpPrefixSidTlvParser(tlvHandler.getType(), tlvHandler));
        regs.add(context.registerBgpPrefixSidTlvSerializer(Ipv6SidTlv.class, tlvHandler));
        return regs;
    }

}
