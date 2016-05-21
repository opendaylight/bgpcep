/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common {@link org.opendaylight.protocol.bgp.rib.spi.RIBSupport} class for IPv4 and IPv6 addresses.
 */
abstract class AbstractIPRIBSupport extends AbstractRIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIPRIBSupport.class);
    private final QName prefixQname;
    private final QName pathIdQname;
    private final NodeIdentifier pathIdNii;
    private final NodeIdentifier prefixNii;
    private final NodeIdentifier destination;
    private final NodeIdentifier nlriRoutesList;
    private final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects;
    private final Class<? extends AddressFamily> addressFamilyClass;

    protected AbstractIPRIBSupport(final Class<? extends DataObject> prefixClass, final Class<? extends AddressFamily> addressFamilyClass,
        final Class<? extends Routes> cazeClass, final Class<? extends DataObject> containerClass, final Class<? extends Route> listClass,
        final QName destination, final QName prefixesQname) {
        super(cazeClass, containerClass, listClass);
        this.addressFamilyClass = addressFamilyClass;
        this.prefixQname = QName.create(routeQName(), "prefix").intern();
        this.pathIdQname = QName.create(routeQName(), "path-id").intern();
        this.pathIdNii = new NodeIdentifier(this.pathIdQname);
        this.prefixNii = new NodeIdentifier(this.prefixQname);
        this.destination = new NodeIdentifier(destination);
        this.nlriRoutesList = new NodeIdentifier(prefixesQname);
        this.cacheableNlriObjects = ImmutableSet.of(prefixClass);
    }

    private QName prefixQName() {
        return this.prefixQname;
    }

    private QName pathIdQName() {
        return this.pathIdQname;
    }

    protected final NodeIdentifier routePrefixIdentifier() {
        return this.prefixNii;
    }

    protected final NodeIdentifier routePathIdIdentifier() {
        return this.pathIdNii;
    }

    @Nonnull
    @Override
    protected final NodeIdentifier destinationContainerIdentifier() {
        return this.destination;
    }

    @Override
    protected void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
        final ContainerNode destination, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    @Override
    protected void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath, final ContainerNode destination,
        final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    @Override
    protected final MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(this.addressFamilyClass);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        mb.setCNextHop(hop);
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(buildDestination(routes)).build());
        return mb.build();
    }

    @Nonnull
    @Override
    protected final MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(Ipv6AddressFamily.class);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(buildWithdrawnDestination(routes)).build());
        return mb.build();
    }

    @Nonnull
    @Override
    public PathArgument getRouteIdAddPath(final long pathId, final PathArgument routeId) {
        return PathIdUtil.createNiiKey(pathId, routeId, routeQName(), pathIdQName(), prefixQName());
    }

    @Override
    public Long extractPathId(final NormalizedNode<?, ?> data) {
        return PathIdUtil.extractPathId(data, this.routePathIdIdentifier());
    }

    @Nonnull
    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Nonnull
    @Override
    public final ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return this.cacheableNlriObjects;
    }

    @Override
    public final boolean isComplexRoute() {
        return false;
    }

    private void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
        final ContainerNode destination, final ContainerNode attributes, final ApplyRoute function) {
        if (destination != null) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = destination.getChild(this.nlriRoutesList);
            if (maybeRoutes.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> routes = maybeRoutes.get();
                if (routes instanceof UnkeyedListNode) {
                    // Instance identifier to table/(choice routes)/(map of route)
                    // FIXME: cache on per-table basis (in TableContext, for example)
                    final YangInstanceIdentifier base = routesPath.node(routesContainerIdentifier()).node(this.routesListIdentifier);
                    for (final UnkeyedListEntryNode e : ((UnkeyedListNode) routes).getValue()) {
                        final NodeIdentifierWithPredicates routeKey = createRouteKey(e);
                        function.apply(tx, base, routeKey, e, attributes);
                    }
                } else {
                    LOG.warn("Routes {} are not a map", routes);
                }
            }
        }
    }

    /**
     * Prefix and Path Id are path of the key
     *
     * @param prefixes UnkeyedListEntryNode containing route
     * @return Nii with Route Key
     */
    private NodeIdentifierWithPredicates createRouteKey(final UnkeyedListEntryNode prefixes) {
        return PathIdUtil.createNiiKey(prefixes, routeQName(), routePrefixIdentifier(), routePathIdIdentifier());
    }

    protected abstract DestinationType buildDestination(final Collection<MapEntryNode> routes);

    protected abstract DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes);
}
