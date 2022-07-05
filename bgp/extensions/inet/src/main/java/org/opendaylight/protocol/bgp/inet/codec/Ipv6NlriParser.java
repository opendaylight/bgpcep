/*
 * Copyright (c) 2013, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet.codec;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;

public final class Ipv6NlriParser implements NlriParser, NlriSerializer {

    private static DestinationIpv6 prefixes(final ByteBuf nlri, final PeerSpecificParserConstraint constraint,
            final AddressFamily afi, final SubsequentAddressFamily safi) {
        final List<Ipv6Prefixes> prefixes = new ArrayList<>();
        final boolean supported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(afi, safi));
        while (nlri.isReadable()) {
            final Ipv6PrefixesBuilder prefixesBuilder = new Ipv6PrefixesBuilder();
            if (supported) {
                prefixesBuilder.setPathId(PathIdUtil.readPathId(nlri));
            }
            prefixesBuilder.setPrefix(Ipv6Util.prefixForByteBuf(nlri));
            prefixes.add(prefixesBuilder.build());
        }
        return new DestinationIpv6Builder().setIpv6Prefixes(prefixes).build();
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) {
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationIpv6CaseBuilder()
                .setDestinationIpv6(prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) {
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn
                .opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.unreach.nlri.withdrawn.routes
                .destination.type.DestinationIpv6CaseBuilder().setDestinationIpv6(
                prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final AttributesReach pathAttributes1 = pathAttributes.augmentation(AttributesReach.class);
        final AttributesUnreach pathAttributes2 = pathAttributes.augmentation(AttributesUnreach.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes advertizedRoutes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (advertizedRoutes != null && advertizedRoutes.getDestinationType() instanceof DestinationIpv6Case) {
                final DestinationIpv6Case destinationIpv6Case =
                        (DestinationIpv6Case) advertizedRoutes.getDestinationType();
                for (final Ipv6Prefixes ipv6Prefix : destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes()) {
                    PathIdUtil.writePathId(ipv6Prefix.getPathId(), byteAggregator);
                    Ipv6Util.writeMinimalPrefix(ipv6Prefix.getPrefix(), byteAggregator);
                }
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType() instanceof org.opendaylight.yang.gen
                    .v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.unreach.nlri
                    .withdrawn.routes.destination.type.DestinationIpv6Case) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                        .DestinationIpv6Case destinationIpv6Case =
                        (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update
                                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                                .DestinationIpv6Case) withdrawnRoutes.getDestinationType();
                for (final Ipv6Prefixes ipv6Prefix : destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes()) {
                    PathIdUtil.writePathId(ipv6Prefix.getPathId(), byteAggregator);
                    Ipv6Util.writeMinimalPrefix(ipv6Prefix.getPrefix(), byteAggregator);
                }
            }
        }
    }
}