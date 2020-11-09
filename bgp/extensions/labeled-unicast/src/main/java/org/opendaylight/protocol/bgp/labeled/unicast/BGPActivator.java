/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv4NextHopParserSerializer;
import org.opendaylight.protocol.bgp.inet.codec.nexthop.Ipv6NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
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
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true, service = BGPExtensionProviderActivator.class,
           property = "type=org.opendaylight.protocol.bgp.labeled.unicast.BGPActivator")
@MetaInfServices(value = BGPExtensionProviderActivator.class)
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    private static final int LABELED_UNICAST_SAFI = 4;

    @Inject
    public BGPActivator() {
        // Exposed for DI
    }

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final LUNlriParser luNlriParser = new LUNlriParser();
        final LabelIndexTlvParser labelHandler = new LabelIndexTlvParser();
        final OriginatorSrgbTlvParser originatorHandler = new OriginatorSrgbTlvParser();

        return List.of(
            context.registerSubsequentAddressFamily(LabeledUnicastSubsequentAddressFamily.class, LABELED_UNICAST_SAFI),
            context.registerNlriParser(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                luNlriParser, new Ipv4NextHopParserSerializer(), Ipv4NextHopCase.class),
            context.registerNlriParser(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                luNlriParser, new Ipv6NextHopParserSerializer(), Ipv6NextHopCase.class),
            context.registerNlriSerializer(LabeledUnicastRoutes.class, luNlriParser),
            context.registerBgpPrefixSidTlvParser(labelHandler.getType(), labelHandler),
            context.registerBgpPrefixSidTlvParser(originatorHandler.getType(), originatorHandler),
            context.registerBgpPrefixSidTlvSerializer(LuLabelIndexTlv.class, labelHandler),
            context.registerBgpPrefixSidTlvSerializer(LuOriginatorSrgbTlv.class, originatorHandler));
    }
}
