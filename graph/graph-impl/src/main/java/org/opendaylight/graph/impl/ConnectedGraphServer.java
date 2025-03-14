/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.GraphTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.GraphTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.GraphBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Empty;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods
 * required to manage the network representation elements in the datastore.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
@Singleton
@Component(immediate = true, service = ConnectedGraphProvider.class)
public final class ConnectedGraphServer implements ConnectedGraphProvider, FutureCallback<Empty>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedGraphServer.class);
    private static final @NonNull DataObjectIdentifier<GraphTopology> GRAPH_TOPOLOGY_IDENTIFIER =
        DataObjectIdentifier.builder(GraphTopology.class).build();

    private final Map<GraphKey, ConnectedGraphImpl> graphs = new HashMap<>();
    private final DataBroker dataBroker;

    private TransactionChain chain = null;

    @Inject
    @Activate
    public ConnectedGraphServer(@Reference final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        initTransactionChain();
        initOperationalGraphModel();
        LOG.info("Graph Model Server started");
    }

    /**
     * Remove the Operation Graph Model and destroy the transaction chain.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        destroyOperationalGraphModel();
        destroyTransactionChain();
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for Graph Model Server {}", this);
        checkState(chain == null, "Transaction chain has to be closed before being initialized");
        chain = dataBroker.createMergingTransactionChain();
        chain.addCallback(this);
    }

    /**
     * Initialize GraphModel tree at Data Store top-level.
     */
    private synchronized void initOperationalGraphModel() {
        requireNonNull(chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        LOG.info("Create Graph Model at top level in Operational DataStore: {}", GRAPH_TOPOLOGY_IDENTIFIER);
        trans.put(LogicalDatastoreType.OPERATIONAL, GRAPH_TOPOLOGY_IDENTIFIER, new GraphTopologyBuilder().build());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize GraphModel {} (transaction {}) by listener {}",
                    GRAPH_TOPOLOGY_IDENTIFIER, trans.getIdentifier(), ConnectedGraphServer.this, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided.
     */
    private synchronized FluentFuture<? extends CommitInfo> destroyOperationalGraphModel() {
        requireNonNull(chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, GRAPH_TOPOLOGY_IDENTIFIER);
        final FluentFuture<? extends CommitInfo> future = trans.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Operational GraphModel removed {}", GRAPH_TOPOLOGY_IDENTIFIER);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to reset operational GraphModel {} (transaction {})", GRAPH_TOPOLOGY_IDENTIFIER,
                    trans.getIdentifier(), throwable);
            }
        }, MoreExecutors.directExecutor());

        /* Clear Connected Graph */
        for (ConnectedGraph graph : graphs.values()) {
            ((ConnectedGraphImpl) graph).clear();
        }
        return future;
    }

    /**
     * Destroy the current transaction chain.
     */
    private synchronized void destroyTransactionChain() {
        if (chain != null) {
            LOG.debug("Destroy transaction chain for GraphModel {}", this);
            chain = null;
        }
    }

    /**
     * Reset the transaction chain only so that the PingPong transaction chain
     * will become usable again. However, there will be data loss if we do not
     * apply the previous failed transaction again
     */
    protected synchronized void resetTransactionChain() {
        LOG.debug("Resetting transaction chain for Graph builder");
        destroyTransactionChain();
        initTransactionChain();
    }

    /**
     *  DataStore Instance Identifier creation for the various Graph components.
     */
    private static WithKey<Graph, GraphKey> getGraphInstanceIdentifier(final String name) {
        return GRAPH_TOPOLOGY_IDENTIFIER.toBuilder().child(Graph.class, new GraphKey(name)).build();
    }

    private static WithKey<Vertex, VertexKey> getVertexInstanceIdentifier(final Graph graph, final Vertex vertex) {
        return GRAPH_TOPOLOGY_IDENTIFIER.toBuilder()
            .child(Graph.class, graph.key())
            .child(Vertex.class, vertex.key())
            .build();
    }

    private static WithKey<Edge, EdgeKey> getEdgeInstanceIdentifier(final Graph graph, final Edge edge) {
        return GRAPH_TOPOLOGY_IDENTIFIER.toBuilder()
            .child(Graph.class, graph.key())
            .child(Edge.class, edge.key())
            .build();
    }

    private static WithKey<Prefix, PrefixKey> getPrefixInstanceIdentifier(final Graph graph, final Prefix prefix) {
        return GRAPH_TOPOLOGY_IDENTIFIER.toBuilder()
            .child(Graph.class, graph.key())
            .child(Prefix.class, prefix.key())
            .build();
    }

    /**
     * Add Graph or Graph components to the Data Store.
     *
     * @param <T>   As a generic method, T must be a Graph, Vertex, Edge or Prefix.
     * @param id    Instance Identifier of the Data Object
     * @param data  Data Object (Graph, Vertex, Edge or Prefix)
     * @param info  Information to be logged
     */
    private synchronized <T extends DataObject> void addToDataStore(final DataObjectIdentifier<T> id, final T data,
            final String info) {
        final var tx = chain.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, id, data);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("GraphModel: {} has been published in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GrahModel: Cannot write {} to the operational datastore (transaction: {})", info,
                        tx.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Update Graph components (Vertex, Edge or Prefix ) to the Data Store. Old value identified by its Instance ID
     * will be remove first before adding the new value.
     *
     * @param <T>   As a generic method, T must be a Vertex, Edge or Prefix.
     * @param id    Instance Identifier of the Data Object
     * @param data  Data Object (Vertex, Edge or Prefix)
     * @param old   Instance Identifier of the previous version of the Data Object
     * @param info  Information to be logged
     */
    private synchronized <T extends DataObject> void updateToDataStore(final DataObjectIdentifier<T> id, final T data,
            final DataObjectIdentifier<T> old, final String info) {
        final var tx = chain.newWriteOnlyTransaction();
        if (old != null) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, old);
        }
        tx.put(LogicalDatastoreType.OPERATIONAL, id, data);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("GraphModel: {} has been published in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GrahModel: Cannot write {} to the operational datastore (transaction: {})", info,
                        tx.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Remove Graph or Graph components to the Data Store.
     *
     * @param <T>  As a generic method, T must be a Graph, Vertex, Edge or Prefix.
     * @param id   Instance Identifier of the Data Object
     * @param info Information to be logged
     */
    private synchronized <T extends DataObject> void removeFromDataStore(final DataObjectIdentifier<T> id,
            final String info) {
        final var tx = chain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, id);
        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("GraphModel: {} has been deleted in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GraphModel: Cannot delete {} to the operational datastore (transaction: {})", info,
                        tx.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Clear Graph. This method is used by the Connected Graph to clear associated Graph.
     *
     * @param graph Graph associated to the Connected Graph
     */
    public void clearGraph(final Graph graph) {
        checkArgument(graph != null, "Provided Graph is a null object");
        removeFromDataStore(getGraphInstanceIdentifier(graph.getName()), "Graph(" + graph.getName() + ")");
        graphs.remove(graph.key());
    }

    /**
     * Add Vertex to existing Graph. Old vertex is remove first. This method is called when a Connected Vertex is
     * created (See addVertex() method from ConnectedGraph Interface).
     *
     * @param graph   Graph where the vertex will be stored
     * @param vertex  Vertex to be inserted in the graph
     * @param old     Old vertex when performing an update. Must be null for a simple addition
     */
    public void addVertex(final Graph graph, final Vertex vertex, final Vertex old) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(vertex != null, "Provided Vertex is a null object");
        /* Remove old Vertex if it exists before storing the new Vertex */
        final var oldId = old != null ? getVertexInstanceIdentifier(graph, old) : null;
        updateToDataStore(getVertexInstanceIdentifier(graph, vertex), vertex, oldId,
                "Vertex(" + vertex.getName() + ")");
    }

    /**
     * Remove Vertex to existing Graph. This method is called when a Connected Vertex is removed (See deleteVertex()
     * method from ConnectedGraph Interface).
     *
     * @param graph   Graph where the vertex is stored
     * @param vertex  Vertex to be removed
     */
    public void deleteVertex(final Graph graph, final Vertex vertex) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(vertex != null, "Provided Vertex is a null object");
        removeFromDataStore(getVertexInstanceIdentifier(graph, vertex), "Vertex(" + vertex.getName() + ")");
    }

    /**
     * Add Edge to existing Graph. Old edge is remove first. This method is called when a Connected Edge is
     * created (See addEdge() method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the edge will be stored
     * @param edge   Edge to be inserted in the graph
     * @param old    Old edge when performing an update. Must be null for a simple addition
     */
    public void addEdge(final Graph graph, final Edge edge, final Edge old) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(edge != null, "Provided Edge is a null object");
        /* Remove old Edge if it exists before storing the new Edge */
        final var oldId = old != null ? getEdgeInstanceIdentifier(graph, old) : null;
        updateToDataStore(getEdgeInstanceIdentifier(graph, edge), edge, oldId, "Edge(" + edge.getName() + ")");
    }

    /**
     * Remove Edge to existing Graph. This method is called when a Connected Edge is removed (See deleteEdge()
     * method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the edge is stored
     * @param edge   Edge to be removed
     */
    public void deleteEdge(final Graph graph, final Edge edge) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(edge != null, "Provided Edge is a null object");
        removeFromDataStore(getEdgeInstanceIdentifier(graph, edge), "Edge(" + edge.getName() + ")");
    }

    /**
     * Add Prefix to existing Graph. This method is called when a Prefix is added to a Connected Vertex
     * (See addPrefix() method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the prefix will be stored
     * @param prefix Prefix to be interted in the graph
     */
    public void addPrefix(final Graph graph, final Prefix prefix) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(prefix != null, "Provided Prefix is a null object");
        addToDataStore(getPrefixInstanceIdentifier(graph, prefix), prefix, "Prefix(" + prefix.getPrefix() + ")");
    }

    /**
     * Remove Prefix to existing Graph. This method is called when a Prefix is removed from a Connected Vertex
     * (See deletePrefix() method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the prefix is stored
     * @param prefix Prefix to be removed
     */
    public void deletePrefix(final Graph graph, final Prefix prefix) {
        checkArgument(graph != null, "Provided Graph is a null object");
        checkArgument(prefix != null, "Provided Prefix is a null object");
        removeFromDataStore(getPrefixInstanceIdentifier(graph, prefix), "Prefix(" + prefix.getPrefix() + ")");
    }

    @Override
    public synchronized void onFailure(final Throwable cause) {
        LOG.error("GraphModel builder for {} failed", GRAPH_TOPOLOGY_IDENTIFIER, cause);
    }

    @Override
    public void onSuccess(final Empty result) {
        LOG.info("GraphModel builder for {} shut down", GRAPH_TOPOLOGY_IDENTIFIER);
    }

    @Override
    public List<ConnectedGraph> getConnectedGraphs() {
        return new ArrayList<>(graphs.values());
    }

    @Override
    public ConnectedGraph getConnectedGraph(final GraphKey key) {
        return graphs.get(key);
    }

    @Override
    public ConnectedGraph getConnectedGraph(final String name) {
        return graphs.get(new GraphKey(name));
    }

    @Override
    public Graph getGraph(final GraphKey key) {
        if (graphs.containsKey(key)) {
            return graphs.get(key).getGraph();
        } else {
            return null;
        }
    }

    @Override
    public Graph getGraph(final String name) {
        return getGraph(new GraphKey(name));
    }

    @Override
    public ConnectedGraph createConnectedGraph(final String name, final DomainScope scope) {
        Graph graph = new GraphBuilder()
                .setName(name)
                .setDomainScope(scope)
                .build();
        addToDataStore(getGraphInstanceIdentifier(name), graph, "Graph(" + name + ")");
        ConnectedGraphImpl cgraph = new ConnectedGraphImpl(graph, this);
        graphs.put(graph.key(), cgraph);
        return cgraph;
    }

    @Override
    public ConnectedGraph addGraph(final Graph graph) {
        checkArgument(graph != null, "Provided Graph is a null object");
        addToDataStore(getGraphInstanceIdentifier(graph.getName()), graph, "Graph(" + graph.getName() + ")");
        ConnectedGraphImpl cgraph = new ConnectedGraphImpl(graph, this);
        graphs.put(graph.key(), cgraph);
        return cgraph;
    }

    @Override
    public void deleteGraph(final GraphKey key) {
        checkArgument(key != null, "Provided Graph Key is a null object");
        ConnectedGraphImpl cgraph = graphs.get(key);
        /*
         * Remove the corresponding Connected Graph which will delete the graph
         * by calling clearGraph() method (see above)
         */
        if (cgraph != null) {
            cgraph.clear();
        }
    }
}
