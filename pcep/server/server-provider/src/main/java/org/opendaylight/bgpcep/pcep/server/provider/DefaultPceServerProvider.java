/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.bgpcep.pcep.server.PathManager;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Singleton
@Component(immediate = true)
public final class DefaultPceServerProvider implements PceServerProvider {
    private final ConnectedGraphProvider graphProvider;
    private final PathComputationProvider algoProvider;
    private final NetworkTopologyPcepService ntps;
    private volatile PathManagerProvider pathManager;
    private final DataBroker dataBroker;
    private volatile ConnectedGraph tedGraph;

    @Inject
    @Activate
    public DefaultPceServerProvider(@Reference final ConnectedGraphProvider graphProvider,
            @Reference final PathComputationProvider pathComputationProvider,
            @Reference final DataBroker dataBroker,
            @Reference final RpcConsumerRegistry rpcConsumerRegistry) {
        this.graphProvider = requireNonNull(graphProvider);
        this.algoProvider = requireNonNull(pathComputationProvider);
        this.dataBroker = requireNonNull(dataBroker);
        this.ntps = rpcConsumerRegistry.getRpcService(NetworkTopologyPcepService.class);
        this.pathManager = new PathManagerProvider(this.dataBroker, this.ntps, this);
    }

    @Override
    public @Nullable PathComputationImpl getPathComputation() {
        /* Check that we have a valid graph */
        final ConnectedGraph graph = getTedGraph();
        return graph == null ? null : new PathComputationImpl(tedGraph, algoProvider);
    }

    @Override
    public @Nullable ConnectedGraph getTedGraph() {
        /* Leave a chance to get a valid Graph in case of late fulfillment */
        if (tedGraph == null) {
            setTedGraph();
        }
        return tedGraph;
    }

    @Override
    public PathManager getPathManager() {
        return this.pathManager;
    }

    /**
     * Set Traffic Engineering Graph. This method is necessary as the TedGraph could be available
     * after the PathComputationFactory start e.g. manual insertion of a ted Graph, or late tedGraph fulfillment
     * from BGP Link State.
     */
    private void setTedGraph() {
        tedGraph = graphProvider.getConnectedGraphs().stream()
            .filter(graph -> graph.getGraph().getName().startsWith("ted://"))
            .findFirst()
            .orElse(null);
    }
}
