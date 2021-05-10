/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Activator for registering linkstate extensions to BGP parser.
 */
@Singleton
@Component(immediate = true, service = BGPExtensionProviderActivator.class,
           property = "type=org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator")
@MetaInfServices(value = BGPExtensionProviderActivator.class)
public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
    private static final int LINKSTATE_AFI = 16388;
    private static final int LINKSTATE_SAFI = 71;

    private final boolean ianaLinkstateAttributeType;
    private final RSVPTeObjectRegistry rsvpTeObjectRegistry;

    public BGPActivator() {
        this(ServiceLoader.load(RSVPExtensionConsumerContext.class).findFirst().orElseThrow(
            () -> new IllegalStateException("Cannot find an RSVPExtensionConsumerContext implementation")));
    }

    @Activate
    @Inject
    public BGPActivator(final @Reference RSVPExtensionConsumerContext rsvpExtensions) {
        this(rsvpExtensions, true);
    }

    public BGPActivator(final RSVPExtensionConsumerContext rsvpExtensions, final boolean ianaLinkstateAttributeType) {
        this.rsvpTeObjectRegistry = rsvpExtensions.getRsvpRegistry();
        this.ianaLinkstateAttributeType = ianaLinkstateAttributeType;
    }

    @Override
    protected List<Registration> startImpl(final BGPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();


        regs.add(context.registerAddressFamily(LinkstateAddressFamily.class, LINKSTATE_AFI));
        regs.add(context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, LINKSTATE_SAFI));

        final NextHopParserSerializer linkstateNextHopParser = new NextHopParserSerializer() {
        };
        final LinkstateNlriParser parser = new LinkstateNlriParser();
        regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            parser, linkstateNextHopParser, Ipv4NextHopCase.class, Ipv6NextHopCase.class));
        regs.add(context.registerNlriSerializer(LinkstateRoutes.class, parser));

        regs.add(context.registerAttributeSerializer(Attributes1.class,
            new LinkstateAttributeParser(this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry)));
        final LinkstateAttributeParser linkstateAttributeParser = new LinkstateAttributeParser(
            this.ianaLinkstateAttributeType, this.rsvpTeObjectRegistry);
        regs.add(context.registerAttributeParser(linkstateAttributeParser.getType(), linkstateAttributeParser));

        return regs;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("ianaLinkstateAttribute", ianaLinkstateAttributeType).toString();
    }
}
