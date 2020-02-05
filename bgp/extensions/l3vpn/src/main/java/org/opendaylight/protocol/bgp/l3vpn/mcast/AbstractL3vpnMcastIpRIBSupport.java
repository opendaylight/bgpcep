/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.mcast;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.L3vpnMcastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.destination.L3vpnMcastDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.l3vpn.mcast.routes.L3vpnMcastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
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
 * Abstract L3VPN Multicast RIBSupport.
 *
 * @author Claudio D. Gasparini
 */
abstract class AbstractL3vpnMcastIpRIBSupport<
        C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C> & L3vpnMcastRoutes>
        extends AbstractRIBSupport<C, S, L3vpnMcastRoute, L3vpnMcastRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractL3vpnMcastIpRIBSupport.class);
    private final NodeIdentifier nlriRoutesList;
    private final NodeIdentifier rdNid;
    private final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param mappingService     Serialization service
     * @param cazeClass          Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param afiClass           address Family Class
     * @param destContainerQname destination container Qname
     * @param destListQname      destinations list Qname
     */
    AbstractL3vpnMcastIpRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<? extends AddressFamily> afiClass,
            final QName destContainerQname,
            final QName destListQname) {
        super(mappingService, cazeClass, containerClass, L3vpnMcastRoute.class, afiClass,
                McastMplsLabeledVpnSubsequentAddressFamily.class, destContainerQname);
        final QNameModule module = BindingReflections.getQNameModule(cazeClass);
        this.nlriRoutesList = NodeIdentifier.create(destListQname);
        this.rdNid = new NodeIdentifier(QName.create(module, "route-distinguisher"));
        this.cacheableNlriObjects = ImmutableSet.of(cazeClass);

    }

    @Override
    public final ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    protected abstract IpPrefix createPrefix(String prefix);

    @Override
    public final L3vpnMcastRoute createRoute(final L3vpnMcastRoute route, final L3vpnMcastRouteKey key,
            final Attributes attributes) {
        final L3vpnMcastRouteBuilder builder;
        if (route != null) {
            builder = new L3vpnMcastRouteBuilder(route);
        } else {
            builder = new L3vpnMcastRouteBuilder();
        }
        return builder.withKey(key).setAttributes(attributes).build();
    }

    @Override
    public final L3vpnMcastRouteKey createRouteListKey(final PathId pathId, final String routeKey) {
        return new L3vpnMcastRouteKey(pathId, routeKey);
    }

    @Override
    protected final Collection<NodeIdentifierWithPredicates> processDestination(
            final DOMDataTreeWriteTransaction tx,
            final YangInstanceIdentifier routesPath,
            final ContainerNode destination,
            final ContainerNode attributes,
            final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination
                    .getChild(nlriRoutesList);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).getValue();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode l3vpnDest : routesList) {
                        final YangInstanceIdentifier.NodeIdentifierWithPredicates routeKey = createRouteKey(l3vpnDest);
                        function.apply(tx, base, routeKey, l3vpnDest, attributes);
                        keys.add(routeKey);
                    }
                    return keys;
                }
                LOG.warn("Routes {} are not a map", routes);
            }
        }
        return Collections.emptyList();
    }

    final List<L3vpnMcastDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractDestinations).collect(Collectors.toList());
    }

    final L3vpnMcastDestination extractDestinations(
            final DataContainerNode<? extends PathArgument> destination) {
        return new L3vpnMcastDestinationBuilder()
                .setRouteDistinguisher(RouteDistinguisherUtil.extractRouteDistinguisher(destination, rdNid))
                .setPrefix(createPrefix(extractPrefix(destination)))
                .setPathId(PathIdUtil.buildPathId(destination, routePathIdNid()))
                .build();
    }

    @Override
    public final PathId extractPathId(final L3vpnMcastRouteKey routeListKey) {
        return routeListKey.getPathId();
    }

    @Override
    public String extractRouteKey(final L3vpnMcastRouteKey routeListKey) {
        return routeListKey.getRouteKey();
    }

    abstract NodeIdentifierWithPredicates createRouteKey(UnkeyedListEntryNode l3vpn);
}
