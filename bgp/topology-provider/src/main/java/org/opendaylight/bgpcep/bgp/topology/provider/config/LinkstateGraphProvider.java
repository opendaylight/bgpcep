/*
 * Copyright (c) 2020 Orange. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider.config;

import static java.util.Objects.requireNonNull;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.LinkstateGraphBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Singleton
@Component(service = {})
public final class LinkstateGraphProvider extends AbstractBgpTopologyProvider {
    private final ConnectedGraphProvider graphProvider;

    @Inject
    @Activate
    public LinkstateGraphProvider(@Reference final BgpTopologyDeployer deployer,
            @Reference final ConnectedGraphProvider graphProvider) {
        super(deployer);
        this.graphProvider = requireNonNull(graphProvider);
        register();
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        unregister();
    }

    @Override
    AbstractTopologyBuilder<?> createTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        return new LinkstateGraphBuilder(dataProvider, locRibReference, topologyId, graphProvider);
    }

    @Override
    public boolean topologyTypeFilter(final Topology topology) {
        final TopologyTypes1 topoAug = getTopologyAug(topology);
        return topoAug != null && topoAug.getBgpLinkstateTopology() != null;
    }
}
