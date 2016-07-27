/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.Ipv6ReachabilityTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public final class Ipv6TopologyProvider extends AbstractBgpTopologyProvider {

    public Ipv6TopologyProvider(final BgpTopologyDeployer deployer) {
        super(deployer);
    }

    @Override
    AbstractTopologyBuilder<?> createTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference, final TopologyId topologyId) {
        return new Ipv6ReachabilityTopologyBuilder(dataProvider, locRibReference, topologyId);
    }

    @Override
    boolean topologyTypeFilter(final TopologyTypes1 topology) {
        return topology.getBgpIpv6ReachabilityTopology() != null;
    }

}
