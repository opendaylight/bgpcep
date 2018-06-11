/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn.mcast.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupportUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv4AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationL3vpnMcastIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.l3vpn.mcast.ipv4.advertized._case.DestinationIpv4L3vpnMcastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv4WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationL3vpnMcastIpv4WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.
 *
 * @author Claudio D. Gasparini
 */
public final class L3vpnMcastIpv4NlriHandler implements NlriParser, NlriSerializer {
    @Override
    public void parseNlri(
            final ByteBuf nlri,
            final MpReachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                .setDestinationType(new DestinationL3vpnMcastIpv4AdvertizedCaseBuilder()
                        .setDestinationIpv4L3vpnMcast(new DestinationIpv4L3vpnMcastBuilder()
                                .setL3vpnMcastDestination(L3vpnMcastNlriSerializer.extractDest(nlri, mPathSupported))
                                .build()).build()).build());
    }


    @Override
    public void parseNlri(
            final ByteBuf nlri,
            final MpUnreachNlriBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final boolean mPathSupported = MultiPathSupportUtil.isTableTypeSupported(constraint,
                new BgpTableTypeImpl(builder.getAfi(), builder.getSafi()));
        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                .setDestinationType(new DestinationL3vpnMcastIpv4WithdrawnCaseBuilder().setDestinationIpv4L3vpnMcast(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417
                                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.l3vpn
                                .mcast.ipv4.withdrawn._case.DestinationIpv4L3vpnMcastBuilder()
                                .setL3vpnMcastDestination(L3vpnMcastNlriSerializer.extractDest(nlri, mPathSupported))
                                .build()).build()).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes,
                "Attribute parameter is not a Attributes object");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.augmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.augmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = pathAttributes1.getMpReachNlri().getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof DestinationL3vpnMcastIpv4AdvertizedCase) {
                final DestinationL3vpnMcastIpv4AdvertizedCase reach
                        = (DestinationL3vpnMcastIpv4AdvertizedCase) routes.getDestinationType();
                L3vpnMcastNlriSerializer.serializeNlri(reach.getDestinationIpv4L3vpnMcast().getL3vpnMcastDestination(),
                        byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final WithdrawnRoutes withdrawnRoutes = pathAttributes2.getMpUnreachNlri().getWithdrawnRoutes();
            if (withdrawnRoutes != null
                    && withdrawnRoutes.getDestinationType() instanceof DestinationL3vpnMcastIpv4WithdrawnCase) {
                final DestinationL3vpnMcastIpv4WithdrawnCase reach
                        = (DestinationL3vpnMcastIpv4WithdrawnCase) withdrawnRoutes.getDestinationType();
                L3vpnMcastNlriSerializer.serializeNlri(reach.getDestinationIpv4L3vpnMcast().getL3vpnMcastDestination(),
                        byteAggregator);
            }
        }
    }
}
