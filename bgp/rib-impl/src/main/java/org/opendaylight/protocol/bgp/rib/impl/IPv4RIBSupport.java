/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Class supporting IPv4 unicast RIBs.
 */
final class IPv4RIBSupport extends AbstractIPRIBSupport {
    @VisibleForTesting
    static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final QName PATHID_QNAME = QName.create(Ipv4Route.QNAME, "path-id").intern();
    private static final IPv4RIBSupport SINGLETON = new IPv4RIBSupport();
    private static final ImmutableCollection<Class<? extends DataObject>> CACHEABLE_NLRI_OBJECTS =
            ImmutableSet.<Class<? extends DataObject>>of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.Ipv4Prefix.class);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(Ipv4Routes.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Ipv4Route.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationIpv4.QNAME);
    private final NodeIdentifier ipv4Route = new NodeIdentifier(Ipv4Route.QNAME);
    private final NodeIdentifier nlriRoutesList = new NodeIdentifier(Ipv4Prefixes.QNAME);
    private final NodeIdentifier routeKeyLeaf = new NodeIdentifier(PREFIX_QNAME);
    private final NodeIdentifier pathIdLeaf = new NodeIdentifier(PATHID_QNAME);

    private IPv4RIBSupport() {
        super(Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class);
    }

    static IPv4RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return this.emptyRoutes;
    }

    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return this.destination;
    }

    @Override
    protected NodeIdentifier routeIdentifier() {
        return this.ipv4Route;
    }

    @Override
    protected NodeIdentifier routeKeyLeafIdentifier() {
        return this.routeKeyLeaf;
    }

    @Override
    protected NodeIdentifier nlriRoutesListIdentifier() {
        return this.nlriRoutesList;
    }

    @Override
    protected QName keyLeafQName() {
        return PREFIX_QNAME;
    }

    @Override
    protected QName routeQName() {
        return Ipv4Route.QNAME;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return CACHEABLE_NLRI_OBJECTS;
    }

    @Nonnull
    @Override
    public PathArgument getRouteIdAddPath(final long pathId, final PathArgument routeId) {
        final String prefix = (String) (((NodeIdentifierWithPredicates) routeId).getKeyValues()).get(PREFIX_QNAME);
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(PATHID_QNAME, pathId, PREFIX_QNAME, prefix);

        return new NodeIdentifierWithPredicates(Ipv4Route.QNAME, keyValues);
    }

    @Override
    public long extractPathId(final NormalizedNode<?, ?> data) {
        final NormalizedNode<?, ?> pathId = NormalizedNodes.findNode(data, pathIdLeaf).orNull();
        if (pathId == null) {
            return 0;
        }
        return (Long) pathId.getValue();
    }

    private List<Ipv4Prefixes> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv4Prefixes> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode route : routes) {
            final String prefix = (String) route.getChild(this.routeKeyLeaf).get().getValue();
            final Ipv4PrefixesBuilder prefixBuilder = new Ipv4PrefixesBuilder().setPrefix(new Ipv4Prefix(prefix));
            final Optional<DataContainerChild<? extends PathArgument, ?>> pathIdChild = route.getChild(this.pathIdLeaf);
            if(pathIdChild.isPresent()) {
                prefixBuilder.setPathId(new PathId((Long) pathIdChild.get().getValue()));
            }
            prefs.add(prefixBuilder.build());
        }
        return prefs;
    }

    @Override
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        mb.setCNextHop(hop);
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv4CaseBuilder().setDestinationIpv4(
                new DestinationIpv4Builder().setIpv4Prefixes(extractPrefixes(routes)).build()).build()).build());
        return mb.build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(Ipv4AddressFamily.class);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(
                new DestinationIpv4Builder().setIpv4Prefixes(extractPrefixes(routes)).build()).build()).build());
        return mb.build();
    }
}
