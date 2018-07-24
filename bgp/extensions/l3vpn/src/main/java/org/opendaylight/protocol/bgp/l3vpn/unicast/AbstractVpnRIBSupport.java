/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastIpv4RIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.L3vpnIpRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.route.VpnRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.route.VpnRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.route.VpnRouteKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
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

public abstract class AbstractVpnRIBSupport<C extends Routes & DataObject, S extends ChildOf<? super C> & L3vpnIpRoute>
        extends AbstractRIBSupport<C, S, VpnRoute, VpnRouteKey> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnRIBSupport.class);
    private final NodeIdentifier nlriRoutesListNid;
    private final NodeIdentifier labelStackNid;
    private final NodeIdentifier lvNid;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass      Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     */
    protected AbstractVpnRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<? extends AddressFamily> afiClass,
            final QName vpnDstContainerClassQname) {
        super(mappingService, cazeClass, containerClass, VpnRoute.class, afiClass,
                MplsLabeledVpnSubsequentAddressFamily.class, vpnDstContainerClassQname);
        final QName classQname = BindingReflections.findQName(containerClass).intern();
        final QName vpnDstClassQname = QName.create(classQname, VpnDestination.QNAME.getLocalName());
        this.nlriRoutesListNid = NodeIdentifier.create(vpnDstClassQname);
        this.labelStackNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-stack").intern());
        this.lvNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-value").intern());
    }

    private VpnDestination extractVpnDestination(final DataContainerNode<? extends PathArgument> route) {
        return new VpnDestinationBuilder()
                .setPrefix(createPrefix(extractPrefix(route)))
                .setLabelStack(LabeledUnicastIpv4RIBSupport.extractLabel(route, this.labelStackNid, this.lvNid))
                .setRouteDistinguisher(extractRouteDistinguisher(route))
                .setPathId(PathIdUtil.buildPathId(route, routePathIdNid()))
                .build();
    }

    protected abstract IpPrefix createPrefix(String prefix);

    protected abstract DestinationType getAdvertisedDestinationType(List<VpnDestination> dests);

    protected abstract DestinationType getWithdrawnDestinationType(List<VpnDestination> dests);

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return getAdvertisedDestinationType(extractRoutes(routes));
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return getWithdrawnDestinationType(extractRoutes(routes));
    }

    private List<VpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractVpnDestination).collect(Collectors.toList());
    }

    @Override
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes =
                    destination.getChild(this.nlriRoutesListNid);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    final UnkeyedListNode routeListNode = (UnkeyedListNode) routes;
                    LOG.debug("{} routes are found", routeListNode.getSize());
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    for (final UnkeyedListEntryNode e : routeListNode.getValue()) {
                        final NodeIdentifierWithPredicates key = createRouteKey(e);
                        LOG.debug("Route {} is processed.", key);
                        function.apply(tx, base, key, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        } else {
            LOG.debug("Destination is null.");
        }
    }

    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode l3vpn) {
        final ByteBuf buffer = Unpooled.buffer();
        final VpnDestination dests = new VpnDestinationBuilder().setPrefix(createPrefix(extractPrefix(l3vpn)))
            .setRouteDistinguisher(extractRouteDistinguisher(l3vpn)).build();
        final ByteBuf nlriByteBuf = Unpooled.buffer();

        for (final VpnDestination dest : Collections.singletonList(dests)) {
            final IpPrefix prefix = dest.getPrefix();
            LOG.debug("Serializing Nlri: VpnDestination={}, IpPrefix={}", dest, prefix);
            AbstractVpnNlriParser.serializeLengtField(prefix, null, nlriByteBuf);
            RouteDistinguisherUtil.serializeRouteDistinquisher(dest.getRouteDistinguisher(), nlriByteBuf);
            Preconditions.checkArgument(prefix.getIpv6Prefix() != null || prefix.getIpv4Prefix() != null,
                    "Ipv6 or Ipv4 prefix is missing.");
            LUNlriParser.serializePrefixField(prefix, nlriByteBuf);
        }
        buffer.writeBytes(nlriByteBuf);

        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf =
                l3vpn.getChild(routePathIdNid());
        return PathIdUtil.createNidKey(routeQName(), routeKeyQName(),
                pathIdQName(), ByteArray.encodeBase64(buffer), maybePathIdLeaf);
    }

    @Override
    public final VpnRoute createRoute(final VpnRoute route, final String vpnRouteKey,
            final long pathId, final Attributes attributes) {
        final VpnRouteBuilder builder;
        if (route != null) {
            builder = new VpnRouteBuilder(route);
        } else {
            builder = new VpnRouteBuilder();
        }
        return builder.withKey(new VpnRouteKey(new PathId(pathId), vpnRouteKey)).setAttributes(attributes).build();
    }

    @Override
    public VpnRouteKey createRouteListKey(final long pathId, final String vpnRouteKey) {
        return new VpnRouteKey(new PathId(pathId), vpnRouteKey);
    }
}
