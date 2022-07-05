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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastIpv4RIBSupport;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.L3vpnIpRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.route.VpnRoute;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVpnRIBSupport<C extends Routes & DataObject, S extends ChildOf<? super C> & L3vpnIpRoute>
        extends AbstractRIBSupport<C, S, VpnRoute> {
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
            final Class<S> containerClass, final QName containerQName,
            final AddressFamily afiClass,
            final QName vpnDstContainerClassQname) {
        super(mappingService, cazeClass, containerClass, VpnRoute.class, afiClass,
                MplsLabeledVpnSubsequentAddressFamily.VALUE, vpnDstContainerClassQname);
        this.nlriRoutesListNid = NodeIdentifier.create(VpnDestination.QNAME.bindTo(containerQName.getModule())
            .intern());
        this.labelStackNid = NodeIdentifier.create(QName.create(containerQName, "label-stack").intern());
        this.lvNid = NodeIdentifier.create(QName.create(containerQName, "label-value").intern());
    }

    private VpnDestination extractVpnDestination(final DataContainerNode route) {
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
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataTreeWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute function) {
        if (destination != null) {
            final DataContainerChild routes = destination.childByArg(this.nlriRoutesListNid);
            if (routes != null) {
                if (routes instanceof UnkeyedListNode routeListNode) {
                    LOG.debug("{} routes are found", routeListNode.size());
                    final YangInstanceIdentifier base = routesYangInstanceIdentifier(routesPath);
                    final Collection<UnkeyedListEntryNode> routesList = ((UnkeyedListNode) routes).body();
                    final List<NodeIdentifierWithPredicates> keys = new ArrayList<>(routesList.size());
                    for (final UnkeyedListEntryNode vpnDest : routesList) {
                        final NodeIdentifierWithPredicates key = createRouteKey(vpnDest);
                        LOG.debug("Route {} is processed.", key);
                        function.apply(tx, base, key, vpnDest, attributes);
                        keys.add(key);
                    }
                    return keys;
                }
                LOG.warn("Routes {} are not a map", routes);
            }
        } else {
            LOG.debug("Destination is null.");
        }
        return Collections.emptyList();
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

        return PathIdUtil.createNidKey(routeQName(), routeKeyTemplate(),
                ByteArray.encodeBase64(buffer), l3vpn.findChildByArg(routePathIdNid()));
    }
}
