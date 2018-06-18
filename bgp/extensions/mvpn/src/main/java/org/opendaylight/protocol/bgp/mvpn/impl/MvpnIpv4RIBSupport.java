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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.Ipv4NlriHandler;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.MvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.routes.ipv4.MvpnRoutesIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.routes.ipv4.MvpnRoutesIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv4WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;

/**
 * Ipv4 Mvpn RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
final class MvpnIpv4RIBSupport extends AbstractMvpnRIBSupport<MvpnRoutesIpv4Case, MvpnRoutesIpv4> {
    private static final MvpnRoutesIpv4 EMPTY_CONTAINER
            = new MvpnRoutesIpv4Builder().setMvpnRoute(Collections.emptyList()).build();
    private static final MvpnRoutesIpv4Case EMPTY_CASE
            = new MvpnRoutesIpv4CaseBuilder().setMvpnRoutesIpv4(EMPTY_CONTAINER).build();
    private static MvpnIpv4RIBSupport SINGLETON;

    private MvpnIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                MvpnRoutesIpv4Case.class,
                MvpnRoutesIpv4.class,
                Ipv4AddressFamily.class,
                DestinationMvpn.QNAME,
                MvpnDestination.QNAME);
    }

    static synchronized MvpnIpv4RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new MvpnIpv4RIBSupport(mappingService);
        }
        return SINGLETON;
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn
            .destination.MvpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractDestinations).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn
            .destination.MvpnDestination extractDestinations(final DataContainerNode<? extends PathArgument> mvpnDest) {
        return new MvpnDestinationBuilder()
                .setMvpnChoice(extractMvpnChoice(mvpnDest))
                .setPathId(PathIdUtil.buildPathId(mvpnDest, routePathIdNid()))
                .build();
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationMvpnIpv4AdvertizedCaseBuilder().setDestinationMvpn(
                new DestinationMvpnBuilder().setMvpnDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationMvpnIpv4WithdrawnCaseBuilder().setDestinationMvpn(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update
                        .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.mvpn.ipv4.withdrawn
                        ._case.DestinationMvpnBuilder().setMvpnDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    public MvpnRoutesIpv4Case emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public MvpnRoutesIpv4 emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode mvpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination
                .MvpnDestination dest = extractDestinations(mvpn);
        Ipv4NlriHandler.serializeNlri(Collections.singletonList(dest), buffer);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf =
                mvpn.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(),
                pathIdQName(), ByteArray.encodeBase64(buffer), maybePathIdLeaf);
    }
}
