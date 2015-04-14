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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@Deprecated
final class Ipv6AdjRIBsIn extends AbstractAdjRIBs<Ipv6Prefix, Ipv6Route, Ipv6RouteKey> {
    private final InstanceIdentifier<Ipv6Routes> routesBasePath;

    Ipv6AdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        super(basePath);
        this.routesBasePath = basePath.builder().child((Class)Ipv6Routes.class).build();
    }

    @Override
    @Deprecated
    public KeyedInstanceIdentifier<Ipv6Route, Ipv6RouteKey> identifierForKey(final InstanceIdentifier<Tables> basePath, final Ipv6Prefix key) {
        return basePath.child((Class)Ipv6Routes.class).child(Ipv6Route.class, new Ipv6RouteKey(key));
    }

    @Override
    public KeyedInstanceIdentifier<Ipv6Route, Ipv6RouteKey> identifierForKey(final Ipv6Prefix key) {
        return this.routesBasePath.child(Ipv6Route.class, new Ipv6RouteKey(key));
    }

    @Override
    public void addRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpReachNlri nlri,
        final Attributes attributes) {
        final RIBEntryData<Ipv6Prefix, Ipv6Route, Ipv6RouteKey> data = new RIBEntryData<Ipv6Prefix, Ipv6Route, Ipv6RouteKey>(peer, attributes) {
            @Override
            protected Ipv6Route getDataObject(final Ipv6Prefix key, final Ipv6RouteKey id) {
                return new Ipv6RouteBuilder().setKey(id).setAttributes(new AttributesBuilder(attributes).build()).build();
            }
        };
        for (final Ipv6Prefixes id : ((DestinationIpv6Case) nlri.getAdvertizedRoutes().getDestinationType()).getDestinationIpv6().getIpv6Prefixes()) {
            super.add(trans, peer, id.getPrefix(), data);
        }
    }

    @Override
    public void removeRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
        for (final Ipv6Prefixes id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6Case) nlri.getWithdrawnRoutes().getDestinationType()).getDestinationIpv6().getIpv6Prefixes()) {
            super.remove(trans, peer, id.getPrefix());
        }
    }

    @Override
    public void addAdvertisement(final MpReachNlriBuilder builder, final Ipv6Route data) {
        final Attributes a = data.getAttributes();
        if (a != null && a.getCNextHop() != null) {
            builder.setCNextHop(a.getCNextHop());
        }
        final AdvertizedRoutes ar = builder.getAdvertizedRoutes();
        final Ipv6Prefixes p = new Ipv6PrefixesBuilder().setPrefix(data.getPrefix()).build();
        if (ar == null) {
            builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(
                    Lists.newArrayList(p)).build()).build()).build());
        } else {
            ((DestinationIpv6Case) ar.getDestinationType()).getDestinationIpv6().getIpv6Prefixes().add(p);
        }
    }

    @Override
    public void addWithdrawal(final MpUnreachNlriBuilder builder, final Ipv6Prefix id) {
        final WithdrawnRoutes wr = builder.getWithdrawnRoutes();
        final Ipv6Prefixes p = new Ipv6PrefixesBuilder().setPrefix(id).build();
        if (wr == null) {
            builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6CaseBuilder().setDestinationIpv6(new DestinationIpv6Builder().setIpv6Prefixes(
                    Lists.newArrayList(p)).build()).build()).build());
        } else {
            ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationIpv6Case) wr.getDestinationType()).getDestinationIpv6().getIpv6Prefixes().add(p);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyedInstanceIdentifier<Ipv6Route, Ipv6RouteKey> routeIdentifier(final InstanceIdentifier<?> id) {
        return (KeyedInstanceIdentifier<Ipv6Route, Ipv6RouteKey>)id.firstIdentifierOf(Ipv6Route.class);
    }

    @Override
    public Ipv6Prefix keyForIdentifier(final KeyedInstanceIdentifier<Ipv6Route, Ipv6RouteKey> id) {
        return id.getKey().getPrefix();
    }
}
