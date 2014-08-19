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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

final class Ipv4AdjRIBsIn extends AbstractAdjRIBs<Ipv4Prefix, Ipv4Route, Ipv4RouteKey> {
    Ipv4AdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        super(basePath);
    }

    @Override
    public KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> identifierForKey(final InstanceIdentifier<Tables> basePath, final Ipv4Prefix key) {
        return basePath.child(Ipv4Routes.class).child(Ipv4Route.class, new Ipv4RouteKey(key));
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

        for (final Ipv4Prefix id : ((DestinationIpv4Case) nlri.getAdvertizedRoutes().getDestinationType()).getDestinationIpv4().getIpv4Prefixes()) {
            super.add(trans, peer, id, data);
        }
    }

    @Override
    public void removeRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
        for (final Ipv4Prefix id : ((DestinationIpv4Case) nlri.getWithdrawnRoutes().getDestinationType()).getDestinationIpv4().getIpv4Prefixes()) {
            super.remove(trans, peer, id);
        }
    }

    @Override
    public void addAdvertisement(final MpReachNlriBuilder builder, final Ipv4Route data) {
        final AdvertizedRoutes ar = builder.getAdvertizedRoutes();
        if (ar == null) {
            builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(
                    Lists.newArrayList(data.getPrefix())).build()).build()).build());
        } else {
            ((DestinationIpv4) ar.getDestinationType()).getIpv4Prefixes().add(data.getPrefix());
        }
    }

    @Override
    public void addWithdrawal(final MpUnreachNlriBuilder builder, final Ipv4Prefix id) {
        final WithdrawnRoutes wr = builder.getWithdrawnRoutes();
        if (wr == null) {
            builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new DestinationIpv4CaseBuilder().setDestinationIpv4(new DestinationIpv4Builder().setIpv4Prefixes(
                    Lists.newArrayList(id)).build()).build()).build());
        } else {
            ((DestinationIpv4Case) wr.getDestinationType()).getDestinationIpv4().getIpv4Prefixes().add(id);
        }
    }
}
