/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public final class Ipv6NlriParser implements NlriParser {

    private static DestinationIpv6 prefixes(final ByteBuf nlri, final PeerSpecificParserConstraint constraint,
        final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        final List<Ipv6Prefixes> prefixes = new ArrayList<>();
        final boolean supported = MultiPathSupportUtil.isTableTypeSupported(constraint, new BgpTableTypeImpl(afi, safi));
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
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        parseNlri(nlri, builder, null);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        parseNlri(nlri, builder, null);
    }

    @Override
    public void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpReachNlriBuilder builder,
        @Nullable final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationIpv6CaseBuilder().
            setDestinationIpv6(prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }

    @Override
    public void parseNlri(@Nonnull final ByteBuf nlri, @Nonnull final MpUnreachNlriBuilder builder, @Nullable final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder().setDestinationIpv6(
            prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }
}