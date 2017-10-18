/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.bgp.ipv6.reachability.topology.type.BgpIpv6ReachabilityTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class Ipv6ReachabilityTopologyBuilder extends AbstractReachabilityTopologyBuilder<Ipv6Route> {
    public static final TopologyTypes IPV6_TOPOLOGY_TYPE = new TopologyTypesBuilder()
    .addAugmentation(TopologyTypes1.class,
            new TopologyTypes1Builder()
    .setBgpIpv6ReachabilityTopology(new BgpIpv6ReachabilityTopologyBuilder().build()).build())
    .build();

    public Ipv6ReachabilityTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        super(dataProvider, locRibReference, topologyId, IPV6_TOPOLOGY_TYPE, Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
    }

    @Override
    protected Attributes getAttributes(final Ipv6Route value) {
        return value.getAttributes();
    }

    @Override
    protected IpPrefix getPrefix(final Ipv6Route value) {
        return new IpPrefix(value.getKey().getPrefix());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected InstanceIdentifier<Ipv6Route> getRouteWildcard(final InstanceIdentifier<Tables> tablesId) {
        return tablesId.child((Class)Ipv6Routes.class).child(Ipv6Route.class);
    }
}
