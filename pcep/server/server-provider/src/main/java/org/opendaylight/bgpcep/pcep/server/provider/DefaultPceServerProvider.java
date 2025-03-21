/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@Component(immediate = true)
public final class DefaultPceServerProvider implements PceServerProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPceServerProvider.class);

    private final Map<TopologyId, PathManagerProvider> pathManagers = new HashMap<>();
    private final Map<TopologyId, PathManagerListener> pathListeners = new HashMap<>();
    private final Map<TopologyId, PcepTopologyListener> pcepListeners = new HashMap<>();
    private final ConnectedGraphProvider graphProvider;
    private final PathComputationProvider algoProvider;
    private final DataBroker dataBroker;
    private final RpcService rpcService;

    private volatile ConnectedGraph tedGraph;
    private volatile GraphKey graphKey;

    @Inject
    @Activate
    public DefaultPceServerProvider(@Reference final ConnectedGraphProvider graphProvider,
            @Reference final PathComputationProvider pathComputationProvider, @Reference final DataBroker dataBroker,
            @Reference final RpcService rpcService) {
        this.graphProvider = requireNonNull(graphProvider);
        algoProvider = requireNonNull(pathComputationProvider);
        this.dataBroker = requireNonNull(dataBroker);
        this.rpcService = requireNonNull(rpcService);
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        for (PathManagerListener pathListener: pathListeners.values()) {
            pathListener.close();
            pathListener = null;
        }
        pathListeners.clear();
        for (PcepTopologyListener pcepListener: pcepListeners.values()) {
            pcepListener.close();
            pcepListener = null;
        }
        pcepListeners.clear();
        for (PathManagerProvider pathManager: pathManagers.values()) {
            pathManager.close();
            pathManager = null;
        }
        pathManagers.clear();
    }

    private void closeListenerAndManager(final TopologyId key) {
        PathManagerListener pathListener = pathListeners.remove(key);
        if (pathListener != null) {
            pathListener.close();
        }
        PcepTopologyListener pcepListener = pcepListeners.remove(key);
        if (pcepListener != null) {
            pcepListener.close();
        }
        PathManagerProvider pathManager = pathManagers.remove(key);
        if (pathManager != null) {
            pathManager.close();
        }
    }

    @Override
    public @Nullable PathComputation getPathComputation() {
        /* Check that we have a valid graph */
        final ConnectedGraph graph = getTedGraph();
        return graph == null ? null : new PathComputationImpl(tedGraph, algoProvider);
    }

    public ConnectedGraph getTedGraph() {
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
        tedGraph = graphProvider.getConnectedGraph(graphKey);
    }

    @Override
    public void registerPcepTopology(final WithKey<Topology, TopologyKey> topology, final GraphKey key) {
        TopologyId topoKey = requireNonNull(topology).key().getTopologyId();
        graphKey = requireNonNull(key);

        LOG.info("Start PCE Server components for Topology {} with TED {}", topoKey.getValue(), graphKey.getName());

        /* First close current Listener & Manager if there are active */
        closeListenerAndManager(topoKey);

        /* Then create Path Manger */
        PathManagerProvider pathManager = new PathManagerProvider(dataBroker, topology, rpcService, this);

        /* And Listener */
        PathManagerListener pathListener = new PathManagerListener(dataBroker, topology, pathManager);
        PcepTopologyListener pcepListener = new PcepTopologyListener(dataBroker, topology, pathManager);

        /* Finally, register all of them for later deletion */
        pathManagers.put(topoKey, pathManager);
        pathListeners.put(topoKey, pathListener);
        pcepListeners.put(topoKey, pcepListener);
    }

    @Override
    public void unRegisterPcepTopology(final WithKey<Topology, TopologyKey> topology) {
        TopologyId topoKey = requireNonNull(topology).key().getTopologyId();
        graphKey = null;

        LOG.info("Stop PCE Server for Topology {}", topoKey.getValue());
        closeListenerAndManager(topoKey);
    }
}
