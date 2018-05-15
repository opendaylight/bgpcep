/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv4WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv6AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv6WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.
 *
 * @author Claudio D. Gasparini
 */
public final class MvpnNlriHandler implements NlriParser, NlriSerializer {
    @Override
    public void parseNlri(
            final ByteBuf nlri,
            final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final Class<? extends AddressFamily> afi = builder.getAfi();
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));

        DestinationType dst = null;
        if (afi == Ipv4AddressFamily.class) {
            dst = Ipv4NlriHandler.parseIpv4ReachNlri(nlri, mPathSupported);
        } else if (afi == Ipv6AddressFamily.class) {
            dst = Ipv6NlriHandler.parseIpv6ReachNlri(nlri, mPathSupported);
        }

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(dst).build());
    }


    @Override
    public void parseNlri(
            final ByteBuf nlri,
            final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final Class<? extends AddressFamily> afi = builder.getAfi();
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));

        DestinationType dst = null;
        if (afi == Ipv4AddressFamily.class) {
            dst = Ipv4NlriHandler.parseIpv4UnreachNlri(nlri, mPathSupported);
        } else if (afi == Ipv6AddressFamily.class) {
            dst = Ipv6NlriHandler.parseIpv6UnreachNlri(nlri, mPathSupported);
        }

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(dst).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes,
                "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof DestinationMvpnIpv4AdvertizedCase) {
                final DestinationMvpnIpv4AdvertizedCase reach
                        = (DestinationMvpnIpv4AdvertizedCase) routes.getDestinationType();
                Ipv4NlriHandler.serializeNlri(reach.getDestinationMvpn().getMvpnDestination(),
                        byteAggregator);
            } else if (routes != null && routes.getDestinationType() instanceof DestinationMvpnIpv6AdvertizedCase) {
                final DestinationMvpnIpv6AdvertizedCase reach
                        = (DestinationMvpnIpv6AdvertizedCase) routes.getDestinationType();
                Ipv6NlriHandler.serializeNlri(reach.getDestinationMvpn().getMvpnDestination(),
                        byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType()
                    instanceof DestinationMvpnIpv4WithdrawnCase) {
                final DestinationMvpnIpv4WithdrawnCase reach
                        = (DestinationMvpnIpv4WithdrawnCase) withdrawnRoutes.getDestinationType();
                Ipv4NlriHandler.serializeNlri(reach.getDestinationMvpn().getMvpnDestination(),
                        byteAggregator);
            } else if (withdrawnRoutes != null && withdrawnRoutes.getDestinationType()
                    instanceof DestinationMvpnIpv6WithdrawnCase) {
                final DestinationMvpnIpv6WithdrawnCase reach
                        = (DestinationMvpnIpv6WithdrawnCase) withdrawnRoutes.getDestinationType();
                Ipv6NlriHandler.serializeNlri(reach.getDestinationMvpn().getMvpnDestination(),
                        byteAggregator);
            }
        }
    }
}
