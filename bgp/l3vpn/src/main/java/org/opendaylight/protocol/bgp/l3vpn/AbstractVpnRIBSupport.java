/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.labeled.unicast.LabeledUnicastIpv4RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev160413.l3vpn.ip.destination.type.VpnDestinationBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
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
 * @author Kevin Wang
 */
public abstract class AbstractVpnRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVpnRIBSupport.class);
    private final NodeIdentifier nlriRoutesListNid;
    private final NodeIdentifier prefixTypeNid;
    private final NodeIdentifier labelStackNid;
    private final NodeIdentifier lvNid;
    private final NodeIdentifier rdNid;
    private final QName routeKey;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass      Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param listClass      Binding class of the route list, nust not be null;
     */
    protected AbstractVpnRIBSupport(Class<? extends Routes> cazeClass, Class<? extends DataObject> containerClass, Class<? extends Route> listClass,
        Class<? extends AddressFamily> afiClass, final QName vpnDstContainerClassQname) {
        super(cazeClass, containerClass, listClass, afiClass, MplsLabeledVpnSubsequentAddressFamily.class, vpnDstContainerClassQname);
        final QName classQname = BindingReflections.findQName(containerClass).intern();
        routeKey = QName.create(routeQName(), "route-key").intern();
        final QName vpnDstClassQname = QName.create(classQname, VpnDestination.QNAME.getLocalName());
        nlriRoutesListNid = NodeIdentifier.create(vpnDstClassQname);
        prefixTypeNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "prefix").intern());
        labelStackNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-stack").intern());
        lvNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "label-value").intern());
        rdNid = NodeIdentifier.create(QName.create(vpnDstClassQname, "route-distinguisher").intern());
    }

    private VpnDestination extractVpnDestination(DataContainerNode<? extends PathArgument> route) {
        final VpnDestination dst = new VpnDestinationBuilder()
            .setPrefix(extractPrefix(route, prefixTypeNid))
            .setLabelStack(LabeledUnicastIpv4RIBSupport.extractLabel(route, labelStackNid, lvNid))
            .setRouteDistinguisher(extractRouteDistinguisher(route))
            .build();
        return dst;
    }

    protected abstract IpPrefix extractPrefix(final DataContainerNode<? extends PathArgument> route, final NodeIdentifier prefixTypeNid);

    private RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route) {
        if (route.getChild(rdNid).isPresent()) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) route.getChild(rdNid).get().getValue());
        }
        return null;
    }

    protected abstract DestinationType getAdvertisedDestinationType(List<VpnDestination> dests);

    protected abstract DestinationType getWithdrawnDestinationType(List<VpnDestination> dests);

    @Nonnull
    @Override
    protected DestinationType buildDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return getAdvertisedDestinationType(extractRoutes(routes));
    }

    @Nonnull
    @Override
    protected DestinationType buildWithdrawnDestination(@Nonnull final Collection<MapEntryNode> routes) {
        return getWithdrawnDestinationType(extractRoutes(routes));
    }

    private List<VpnDestination> extractRoutes(final Collection<MapEntryNode> routes) {
        return routes.stream().map(this::extractVpnDestination).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Nonnull
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
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesListNid);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    UnkeyedListNode routeListNode = (UnkeyedListNode) routes;
                    LOG.debug("{} routes are found", routeListNode.getSize());
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(routeNid());
                    for (final UnkeyedListEntryNode e : routeListNode.getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        LOG.debug("Route {} is processed.", routeKey);
                        function.apply(tx, base, routeKey, e, attributes);
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

        final VpnDestination dest = extractVpnDestination(l3vpn);
        AbstractVpnNlriParser.serializeNlri(Collections.singletonList(dest), buffer);
        return new NodeIdentifierWithPredicates(routeQName(), this.routeKey, ByteArray.readAllBytes(buffer));
    }
}
