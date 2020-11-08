/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.inet.codec.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6BgpPrefixSidParser;
import org.opendaylight.protocol.bgp.inet.codec.Ipv6NlriParser;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;

@MetaInfServices(value = BGPExtensionProviderActivator.class)
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final Ipv4NlriParser ipv4Codec = new Ipv4NlriParser();
        final Ipv6NlriParser ipv6Codec = new Ipv6NlriParser();
        final Ipv6BgpPrefixSidParser tlvHandler = new Ipv6BgpPrefixSidParser();

        return List.of(
            context.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
                ipv4Codec, new Ipv4NextHopParserSerializer(), Ipv4NextHopCase.class, Ipv6NextHopCase.class),
            context.registerNlriParser(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, ipv6Codec,
                new Ipv6NextHopParserSerializer(), Ipv4NextHopCase.class, Ipv6NextHopCase.class),
            context.registerNlriSerializer(Ipv4Routes.class, ipv4Codec),
            context.registerNlriSerializer(Ipv6Routes.class, ipv6Codec),
            context.registerBgpPrefixSidTlvParser(tlvHandler.getType(), tlvHandler),
            context.registerBgpPrefixSidTlvSerializer(Ipv6SidTlv.class, tlvHandler));
    }
}
