/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class Ipv4NlriParser implements NlriParser, NlriSerializer {

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Nlri, "Attribute parameter is not a Nlri object.");
        final Nlri nlri = (Nlri) attribute;
        for (final Ipv4Prefix ipv4Prefix : nlri.getNlri()) {
            ByteBufWriteUtil.writeMinimalPrefix(ipv4Prefix, byteAggregator);
        }
    }

    private static DestinationIpv4 prefixes(final ByteBuf nlri, final PeerSpecificParserConstraint constraints,
            final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        final List<Ipv4Prefixes> prefixes = new ArrayList<>();
        while (nlri.isReadable()) {
            final Ipv4PrefixesBuilder prefixesBuilder = new Ipv4PrefixesBuilder();
            if (MultiPathSupportUtil.isTableTypeSupported(constraints, new BgpTableTypeImpl(afi, safi))) {
                prefixesBuilder.setPathId(PathIdUtil.readPathId(nlri));
            }
            prefixesBuilder.setPrefix(Ipv4Util.prefixForByteBuf(nlri));
            prefixes.add(prefixesBuilder.build());
        }
        return new DestinationIpv4Builder().setIpv4Prefixes(prefixes).build();
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) {
        parseNlri(nlri, builder, null);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) {
        parseNlri(nlri, builder, null);
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder, final PeerSpecificParserConstraint constraint) {
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder, final PeerSpecificParserConstraint constraint) {
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                prefixes(nlri, constraint, builder.getAfi(), builder.getSafi())).build()).build());
    }
}