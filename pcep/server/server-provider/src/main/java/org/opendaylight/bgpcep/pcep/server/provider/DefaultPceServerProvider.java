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
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Singleton
@Component(immediate = true)
public final class DefaultPceServerProvider implements PceServerProvider, AutoCloseable {
    private final ConnectedGraphProvider graphProvider;
    private final PathComputationProvider algoProvider;
    private final DataBroker dataBroker;
    private final RpcConsumerRegistry rpcRegistry;
    private PathManagerProvider pathManager = null;
    private PathManagerListener pathListener = null;
    private PcepTopologyListener pcepListener = null;
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
        this.rpcRegistry = requireNonNull(rpcConsumerRegistry);
    }

    @Override
    public void close() {
        closeListenerAndManager();
    }

    private void closeListenerAndManager() {
        if (pathListener != null) {
            pathListener.close();
            pathListener = null;
        }
        if (pcepListener != null) {
            pcepListener.close();
            pcepListener = null;
        }
        if (pathManager != null) {
            pathManager.close();
            pathManager = null;
        }
    }

    @Override
    public @Nullable PathComputation getPathComputation() {
        /* Check that we have a valid graph */
        final ConnectedGraph graph = getTedGraph();
        return graph == null ? null : new PathComputationImpl(tedGraph, algoProvider);
    }

    private ConnectedGraph getTedGraph() {
        /* Leave a chance to get a valid Graph in case of late fulfillment */
        if (tedGraph == null) {
            setTedGraph();
        }
        return tedGraph;
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

    @Override
    public void registerPcepTopology(KeyedInstanceIdentifier<Topology, TopologyKey> topology) {
        /* First close current Listener & Manager if there are active */
        closeListenerAndManager();
        /* Then create Path Manger */
        final NetworkTopologyPcepService ntps = rpcRegistry.getRpcService(NetworkTopologyPcepService.class);
        pathManager = new PathManagerProvider(dataBroker, topology, ntps, this);
        /* And Listener */
        pathListener = new PathManagerListener(dataBroker, pathManager);
        pcepListener = new PcepTopologyListener(dataBroker, topology, pathManager);
    }

    @Override
    public void unRegisterPcepTopology() {
        closeListenerAndManager();
    }
}
