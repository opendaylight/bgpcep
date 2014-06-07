/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv6._case.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class Ipv6AdjRIBsIn extends AbstractAdjRIBsIn<Ipv6Prefix, Ipv6Route> {
    Ipv6AdjRIBsIn(final DataModificationTransaction trans, final RibReference rib, final TablesKey key) {
        super(trans, rib, key);
    }

    @Override
    public InstanceIdentifier<Ipv6Route> identifierForKey(final InstanceIdentifier<Tables> basePath, final Ipv6Prefix key) {
        return basePath.builder().child(Ipv6Routes.class).child(Ipv6Route.class, new Ipv6RouteKey(key)).toInstance();
    }

    @Override
    public void addRoutes(final DataModificationTransaction trans, final Peer peer, final MpReachNlri nlri,
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes attributes) {
        final RIBEntryData<Ipv6Prefix, Ipv6Route> data = new RIBEntryData<Ipv6Prefix, Ipv6Route>(attributes) {
            @Override
            protected Ipv6Route getDataObject(final Ipv6Prefix key, final InstanceIdentifier<Ipv6Route> id) {
                return new Ipv6RouteBuilder().setKey(InstanceIdentifier.keyOf(id)).setAttributes(new AttributesBuilder(attributes).build()).build();
            }
        };

        for (final Ipv6Prefix id : ((DestinationIpv6) nlri.getAdvertizedRoutes().getDestinationType()).getIpv6Prefixes()) {
            super.add(trans, peer, id, data);
        }
    }

    @Override
    public void removeRoutes(final DataModificationTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
        for (final Ipv6Prefix id : ((DestinationIpv6) nlri.getWithdrawnRoutes().getDestinationType()).getIpv6Prefixes()) {
            super.remove(trans, peer, id);
        }
    }
}