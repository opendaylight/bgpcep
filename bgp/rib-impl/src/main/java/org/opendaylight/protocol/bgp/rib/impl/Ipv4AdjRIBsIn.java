/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.Lists;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsTransaction;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

final class Ipv4AdjRIBsIn extends AbstractAdjRIBs<Ipv4Prefix, Ipv4Route, Ipv4RouteKey> {
    private final InstanceIdentifier<Ipv4Routes> routesBasePath;

    Ipv4AdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        super(basePath);
        this.routesBasePath = basePath.builder().child((Class)Ipv4Routes.class).build();
    }

    @Override
    @Deprecated
    public KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> identifierForKey(final InstanceIdentifier<Tables> basePath, final Ipv4Prefix key) {
        return basePath.child((Class)Ipv4Routes.class).child(Ipv4Route.class, new Ipv4RouteKey(key));
    }

    @Override
    public KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> identifierForKey(final Ipv4Prefix key) {
        return this.routesBasePath.child(Ipv4Route.class, new Ipv4RouteKey(key));
    }

    @Override
    public void addRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpReachNlri nlri,
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes attributes) {
        final RIBEntryData<Ipv4Prefix, Ipv4Route, Ipv4RouteKey> data = new RIBEntryData<Ipv4Prefix, Ipv4Route, Ipv4RouteKey>(peer, attributes) {
            @Override
            protected Ipv4Route getDataObject(final Ipv4Prefix key, final Ipv4RouteKey id) {
                return new Ipv4RouteBuilder().setKey(id).setAttributes(new AttributesBuilder(attributes).build()).build();
            }
        };

        for (final Ipv4Prefixes id : ((DestinationIpv4Case) nlri.getAdvertizedRoutes().getDestinationType()).getDestinationIpv4().getIpv4Prefixes()) {
            super.add(trans, peer, id.getPrefix(), data);
        }
    }

    @Override
    public void removeRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
        for (final Ipv4Prefixes id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case) nlri.getWithdrawnRoutes().getDestinationType()).getDestinationIpv4().getIpv4Prefixes()) {
            super.remove(trans, peer, id.getPrefix());
        }
    }

    @Override
    public void addAdvertisement(final MpReachNlriBuilder builder, final Ipv4Route data) {
        final Attributes a = data.getAttributes();
        if (a != null && a.getCNextHop() != null) {
            builder.setCNextHop(a.getCNextHop());
        }
        final AdvertizedRoutes ar = builder.getAdvertizedRoutes();
        final Ipv4Prefixes p = new Ipv4PrefixesBuilder().setPrefix(data.getPrefix()).build();
        if (ar == null) {
            builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(Lists.newArrayList(p)).build()).build()).build());
        } else {
            ((DestinationIpv4Case) ar.getDestinationType()).getDestinationIpv4().getIpv4Prefixes().add(p);
        }
    }

    @Override
    public void addWithdrawal(final MpUnreachNlriBuilder builder, final Ipv4Prefix id) {
        final WithdrawnRoutes wr = builder.getWithdrawnRoutes();
        final Ipv4Prefixes p = new Ipv4PrefixesBuilder().setPrefix(id).build();
        if (wr == null) {
            builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(
                    Lists.newArrayList(p)).build()).build()).build());
        } else {
            ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv4Case) wr.getDestinationType()).getDestinationIpv4().getIpv4Prefixes().add(p);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> routeIdentifier(final InstanceIdentifier<?> id) {
        return (KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey>)id.firstIdentifierOf(Ipv4Route.class);
    }

    @Override
    public Ipv4Prefix keyForIdentifier(final KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> id) {
        return id.getKey().getPrefix();
    }
}
