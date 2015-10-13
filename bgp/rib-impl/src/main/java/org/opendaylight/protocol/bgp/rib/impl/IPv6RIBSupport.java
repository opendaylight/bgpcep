/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Class supporting IPv6 unicast RIBs.
 */
final class IPv6RIBSupport extends AbstractIPRIBSupport {
    private static final QName PREFIX_QNAME = QName.cachedReference(QName.create(Ipv6Route.QNAME, "prefix"));
    private static final IPv6RIBSupport SINGLETON = new IPv6RIBSupport();
    private static final ImmutableCollection<Class<? extends DataObject>> CACHEABLE_NLRI_OBJECTS =
            ImmutableSet.<Class<? extends DataObject>>of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.Ipv6Prefix.class);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(Ipv6Routes.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Ipv6Route.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationIpv6.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(Ipv6Route.QNAME);
    private final NodeIdentifier nlriRoutesList = new NodeIdentifier(Ipv6Prefixes.QNAME);
    private final NodeIdentifier routeKeyLeaf = new NodeIdentifier(PREFIX_QNAME);

    private IPv6RIBSupport() {
        super(Ipv6RoutesCase.class, Ipv6Routes.class, Ipv6Route.class);
    }

    static IPv6RIBSupport getInstance() {
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
        return this.route;
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
        return Ipv6Route.QNAME;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return CACHEABLE_NLRI_OBJECTS;
    }

    private List<Ipv6Prefixes> extractPrefixes(final Collection<MapEntryNode> routes) {
        final List<Ipv6Prefixes> prefs = new ArrayList<>(routes.size());
        for (final MapEntryNode ipv6Route : routes) {
            final String prefix = (String) ipv6Route.getChild(this.routeKeyLeaf).get().getValue();
            prefs.add(new Ipv6PrefixesBuilder().setPrefix(new Ipv6Prefix(prefix)).build());
        }
        return prefs;
    }

    @Override
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(Ipv6AddressFamily.class);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        final Ipv6NextHop attributeNextHop = ((Ipv6NextHopCase) hop).getIpv6NextHop();
        final Ipv6NextHopBuilder builder = new Ipv6NextHopBuilder().setGlobal(attributeNextHop.getGlobal());
        final Ipv6Address lickLocal = attributeNextHop.getLinkLocal();
        if( lickLocal != null) {
            builder.setLinkLocal(lickLocal);
        }
        mb.setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(builder.build()).build());
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new DestinationIpv6CaseBuilder().setDestinationIpv6(
                new DestinationIpv6Builder().setIpv6Prefixes(extractPrefixes(routes)).build()).build()).build());
        return mb.build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(Ipv6AddressFamily.class);
        mb.setSafi(UnicastSubsequentAddressFamily.class);
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder().setDestinationIpv6(
                new DestinationIpv6Builder().setIpv6Prefixes(extractPrefixes(routes)).build()).build()).build());
        return mb.build();
    }
}
