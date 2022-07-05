/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.Ipv6NlriHandler;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.MvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.routes.ipv6.MvpnRoutesIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv6AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv6.advertized._case.DestinationMvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv6.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv6WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;

/**
 * Ipv6 Mvpn RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
final class MvpnIpv6RIBSupport extends AbstractMvpnRIBSupport<MvpnRoutesIpv6Case, MvpnRoutesIpv6> {
    MvpnIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                MvpnRoutesIpv6Case.class,
                MvpnRoutesIpv6.class,
                Ipv6AddressFamily.VALUE,
                DestinationMvpn.QNAME,
                MvpnDestination.QNAME);
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn
            .destination.MvpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractDestination).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn
            .destination.MvpnDestination extractDestination(final DataContainerNode mvpnDest) {
        return new MvpnDestinationBuilder()
                .setMvpnChoice(extractMvpnChoice(mvpnDest))
                .setPathId(PathIdUtil.buildPathId(mvpnDest, routePathIdNid()))
                .build();
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationMvpnIpv6AdvertizedCaseBuilder().setDestinationMvpn(
                new DestinationMvpnBuilder().setMvpnDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationMvpnIpv6WithdrawnCaseBuilder().setDestinationMvpn(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.mvpn.ipv6
                        .withdrawn._case.DestinationMvpnBuilder().setMvpnDestination(extractRoutes(routes))
                        .build()).build();
    }

    @Override
    public NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode mvpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417.mvpn.destination
                .MvpnDestination dest = extractDestination(mvpn);
        Ipv6NlriHandler.serializeNlri(List.of(dest), buffer);
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
                ByteArray.encodeBase64(buffer), mvpn.findChildByArg(routePathIdNid()));
    }
}
