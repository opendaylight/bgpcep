/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class LinkstateTopologyProvider extends AbstractBgpTopologyProvider {

    public LinkstateTopologyProvider(final BgpTopologyDeployer deployer) {
        super(deployer);
    }

    @Override
    TopologyReferenceAutoCloseable initiate(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        final LinkstateTopologyBuilder builder = new LinkstateTopologyBuilder(dataProvider, locRibReference, topologyId);
        final ListenerRegistration<AbstractTopologyBuilder<LinkstateRoute>> registration = builder.start(dataProvider, Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
        return new TopologyReferenceAutoCloseable() {
            @Override
            public void close() {
                registration.close();
                builder.close();
            }

            @Override
            public InstanceIdentifier<Topology> getInstanceIdentifier() {
                return builder.getInstanceIdentifier();
            }
        };
    }

    @Override
    boolean topologyTypeFilter(final TopologyTypes1 topology) {
        return topology.getBgpLinkstateTopology() != null;
    }

}
