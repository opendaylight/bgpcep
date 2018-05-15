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
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.Ipv4NlriHandler;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.MvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.bgp.rib.rib.loc.rib.tables.routes.MvpnRoutesIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv4WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.mvpn.routes.MvpnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default constructor. Requires the QName of the container augmented under the routes choice
 * node in instantiations of the rib grouping. It is assumed that this container is defined by
 * the same model which populates it with route grouping instantiation, and by extension with
 * the route attributes container.
 *
 * @author Claudio D. Gasparini
 */
public final class MvpnIpv4RIBSupport extends AbstractMvpnRIBSupport<MvpnRoutesIpv4Case> {
    private static final Logger LOG = LoggerFactory.getLogger(MvpnIpv4RIBSupport.class);
    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(MvpnDestination.QNAME);
    private static final MvpnRoutes EMPTY_CONTAINER
            = new MvpnRoutesBuilder().setMvpnRoute(Collections.emptyList()).build();
    private static final MvpnRoutesIpv4Case EMPTY_CASE
            = new MvpnRoutesIpv4CaseBuilder().setMvpnRoutes(EMPTY_CONTAINER).build();
    private static MvpnIpv4RIBSupport SINGLETON;
    private final NodeIdentifier prefixTypeNid;

    private MvpnIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                MvpnRoutesIpv4Case.class,
                Ipv4AddressFamily.class,
                McastVpnSubsequentAddressFamily.class,
                DestinationMvpn.QNAME);
        this.prefixTypeNid
                = NodeIdentifier.create(QName.create(MvpnDestination.QNAME, "ipv4-prefix").intern());
    }

    static synchronized MvpnIpv4RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new MvpnIpv4RIBSupport(mappingService);
        }
        return SINGLETON;
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn
            .destination.MvpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractMvpnRoute).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn
            .destination.MvpnDestination extractMvpnRoute(final DataContainerNode<? extends PathArgument> mvpnRoute) {
        return new MvpnDestinationBuilder()
                .setIpv4Prefix(new Ipv4Prefix(extractPrefix(mvpnRoute)))
                .setMvpnChoice(extractMvpnChoiceFromRoute(mvpnRoute))
                .setPathId(PathIdUtil.buildPathId(mvpnRoute, routePathIdNid()))
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn
            .destination.MvpnDestination extractDestinations(final DataContainerNode<? extends PathArgument> mvpnDest) {
        return new MvpnDestinationBuilder()
                .setIpv4Prefix(new Ipv4Prefix((String) mvpnDest.getChild(this.prefixTypeNid).get().getValue()))
                .setMvpnChoice(extractMvpnChoiceFromRoute(mvpnDest))
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
    public MvpnRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    protected void processDestination(
            DOMDataWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination
                    .getChild(NLRI_ROUTES_LIST);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                        final YangInstanceIdentifier.NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode mvpn) {
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
