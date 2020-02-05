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
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int LABELED_UNICAST_SAFI = 4;

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>(8);
        final LUNlriParser luNlriParser = new LUNlriParser();

        regs.add(context.registerSubsequentAddressFamily(LabeledUnicastSubsequentAddressFamily.class,
            LABELED_UNICAST_SAFI));

        final Ipv4NextHopParserSerializer ipv4NextHopParser = new Ipv4NextHopParserSerializer();
        final Ipv6NextHopParserSerializer ipv6NextHopParser = new Ipv6NextHopParserSerializer();
        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                luNlriParser, ipv4NextHopParser, Ipv4NextHopCase.class));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                luNlriParser, ipv6NextHopParser, Ipv6NextHopCase.class));

        regs.add(context.registerNlriSerializer(LabeledUnicastRoutes.class, luNlriParser));

        final LabelIndexTlvParser labelHandler = new LabelIndexTlvParser();
        final OriginatorSrgbTlvParser originatorHandler = new OriginatorSrgbTlvParser();
        regs.add(context.registerBgpPrefixSidTlvParser(labelHandler.getType(), labelHandler));
        regs.add(context.registerBgpPrefixSidTlvParser(originatorHandler.getType(), originatorHandler));
        regs.add(context.registerBgpPrefixSidTlvSerializer(LuLabelIndexTlv.class, labelHandler));
        regs.add(context.registerBgpPrefixSidTlvSerializer(LuOriginatorSrgbTlv.class, originatorHandler));

        return regs;
    }
}
