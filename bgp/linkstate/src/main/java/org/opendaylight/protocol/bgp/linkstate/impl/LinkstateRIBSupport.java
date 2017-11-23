/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.linkstate.impl.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LinkstateRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateRIBSupport.class);
    private static final QName ROUTE_KEY = QName.create(LinkstateRoute.QNAME, "route-key").intern();
    private static final LinkstateRIBSupport SINGLETON = new LinkstateRIBSupport();
    private final NodeIdentifier route = new NodeIdentifier(LinkstateRoute.QNAME);
    private final NodeIdentifier nlriRoutesList = new NodeIdentifier(CLinkstateDestination.QNAME);

    private LinkstateRIBSupport() {
        super(LinkstateRoutesCase.class, LinkstateRoutes.class, LinkstateRoute.class, LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            DestinationLinkstate.QNAME);
    }

    public static LinkstateRIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    @Override
    public boolean isComplexRoute() {
        return true;
    }

    @Override
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesList);
            processRoute(maybeRoutes, routesPath, attributes, function, tx);
        }
    }

    private void processRoute(final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes, final YangInstanceIdentifier routesPath,
        final ContainerNode attributes, final ApplyRoute function, final DOMDataWriteTransaction tx) {
        if (maybeRoutes.isPresent()) {
            final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
            if (routes instanceof UnkeyedListNode) {
                final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(this.route);
                for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                    final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                    function.apply(tx, base, routeKey, e, attributes);
                }
            } else {
                LOG.warn("Routes {} are not a map", routes);
            }
        }
    }

    private static NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode linkstate) {
        final ByteBuf buffer = Unpooled.buffer();
        final CLinkstateDestination cLinkstateDestination = LinkstateNlriParser.extractLinkstateDestination(linkstate);
        SimpleNlriTypeRegistry.getInstance().serializeNlriType(cLinkstateDestination, buffer);

        return new NodeIdentifierWithPredicates(LinkstateRoute.QNAME, ROUTE_KEY, ByteArray.readAllBytes(buffer));
    }

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new DestinationLinkstateCaseBuilder().setDestinationLinkstate(
            new DestinationLinkstateBuilder().setCLinkstateDestination(extractRoutes(routes)).build()).build();
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder().
                setCLinkstateDestination(extractRoutes(routes)).build()).build();
    }

    private static List<CLinkstateDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(LinkstateNlriParser::extractLinkstateDestination).collect(Collectors.toList());
    }
}
