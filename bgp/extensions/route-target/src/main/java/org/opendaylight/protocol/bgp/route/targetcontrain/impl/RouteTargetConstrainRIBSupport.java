/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.nlri.ImmutableRouteTargetConstrainNlriRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.bgp.rib.rib.loc.rib.tables.routes.RouteTargetConstrainRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.destination.RouteTargetConstrainDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.RouteTargetConstrainRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.routes.route.target.constrain.routes.RouteTargetConstrainRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationRouteTargetConstrainAdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.constrain.advertized._case.DestinationRouteTargetConstrain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.route.target.constrain.advertized._case.DestinationRouteTargetConstrainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationRouteTargetConstrainWithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Route Target Constrains RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
public final class RouteTargetConstrainRIBSupport
        extends AbstractRIBSupport<RouteTargetConstrainRoutesCase, RouteTargetConstrainRoutes,
        RouteTargetConstrainRoute> {
    private static final Logger LOG = LoggerFactory.getLogger(RouteTargetConstrainRIBSupport.class);

    private static final NodeIdentifier NLRI_ROUTES_LIST = NodeIdentifier.create(RouteTargetConstrainDestination.QNAME);
    private static final String ORIGIN_AS = "origin-as";
    private final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects
            = ImmutableSet.of(RouteTargetConstrainRoutesCase.class);
    private final NodeIdentifier originAsNid;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param mappingService Serialization service
     */
    public RouteTargetConstrainRIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                RouteTargetConstrainRoutesCase.class,
                RouteTargetConstrainRoutes.class,
                RouteTargetConstrainRoute.class,
                Ipv4AddressFamily.VALUE,
                RouteTargetConstrainSubsequentAddressFamily.VALUE,
                DestinationRouteTargetConstrain.QNAME);
        originAsNid = new NodeIdentifier(QName.create(routeQName(), ORIGIN_AS).intern());
    }

    @Override
    public ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects() {
        return cacheableNlriObjects;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationRouteTargetConstrainAdvertizedCaseBuilder().setDestinationRouteTargetConstrain(
                new DestinationRouteTargetConstrainBuilder()
                        .setRouteTargetConstrainDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new DestinationRouteTargetConstrainWithdrawnCaseBuilder()
                .setDestinationRouteTargetConstrain(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                        .yang.bgp.route.target.constrain.rev180618.update.attributes.mp.unreach.nlri.withdrawn.routes
                        .destination.type.destination.route.target.constrain.withdrawn._case
                        .DestinationRouteTargetConstrainBuilder()
                        .setRouteTargetConstrainDestination(extractRoutes(routes)).build()).build();
    }

    private List<RouteTargetConstrainDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractDestination).collect(Collectors.toList());
    }

    private RouteTargetConstrainDestination extractDestination(final DataContainerNode rtDest) {
        final RouteTargetConstrainDestinationBuilder builder = new RouteTargetConstrainDestinationBuilder()
                .setPathId(PathIdUtil.buildPathId(rtDest, routePathIdNid()))
                .setRouteTargetConstrainChoice(extractRouteTargetChoice(rtDest));
        final Optional<Object> originAs = NormalizedNodes
                .findNode(rtDest, originAsNid).map(NormalizedNode::body);
        originAs.ifPresent(o -> builder.setOriginAs(new AsNumber((Uint32) o)));
        return builder.build();
    }

    private RouteTargetConstrainChoice extractRouteTargetChoice(final DataContainerNode route) {
        final DataObject nn = mappingService.fromNormalizedNode(routeDefaultYii, route).getValue();
        return ((RouteTargetConstrainRoute) nn).getRouteTargetConstrainChoice();
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(
            final DOMDataTreeWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final DataContainerChild routes = destination.childByArg(NLRI_ROUTES_LIST);
            if (routes != null) {
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).body();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode rtDest : routesList) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(rtDest);
                        function.apply(tx, base, routeKey, rtDest, attributes);
                        keys.add(routeKey);
                    }
                    return keys;
                }
                LOG.warn("Routes {} are not a map", routes);
            }
        }
        return Collections.emptyList();
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode routeTarget) {
        final ByteBuf buffer = Unpooled.buffer();
        final RouteTargetConstrainDestination dest = extractDestination(routeTarget);
        buffer.writeBytes(ImmutableRouteTargetConstrainNlriRegistry.getInstance()
                .serializeRouteTargetConstrain(dest.getRouteTargetConstrainChoice()));
        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
                ByteArray.encodeBase64(buffer), routeTarget.findChildByArg(routePathIdNid()));
    }
}
