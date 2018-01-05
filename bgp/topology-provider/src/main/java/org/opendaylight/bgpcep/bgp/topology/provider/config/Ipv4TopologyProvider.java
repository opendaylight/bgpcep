/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.Ipv4ReachabilityTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public final class Ipv4TopologyProvider extends AbstractBgpTopologyProvider {

    public Ipv4TopologyProvider(final BgpTopologyDeployer deployer) {
        super(deployer);
    }

    @Override
    public boolean topologyTypeFilter(final Topology topology) {
        final TopologyTypes1 topoAug = getTopologyAug(topology);
        return topoAug != null && topoAug.getBgpIpv4ReachabilityTopology() != null;
    }

    @Override
    AbstractTopologyBuilder<?> createTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        return new Ipv4ReachabilityTopologyBuilder(dataProvider, locRibReference, topologyId);
    }

}
