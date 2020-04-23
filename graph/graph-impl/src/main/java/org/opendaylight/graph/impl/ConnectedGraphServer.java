/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.GraphTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.GraphTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.GraphBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods
 * required to manage the network representation elements in the datastore.
 *
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */

public class ConnectedGraphServer implements ConnectedGraphProvider, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectedGraphServer.class);
    private final DataBroker dataBroker;
    private final InstanceIdentifier<GraphTopology> graphTopologyIdentifier;
    private TransactionChain chain = null;

    private final HashMap<GraphKey, ConnectedGraphImpl> graphs = new HashMap<>();

    public ConnectedGraphServer(final DataBroker dataBroker) {
        LOG.info("Create Graph Model Server");
        this.dataBroker = dataBroker;
        this.graphTopologyIdentifier = InstanceIdentifier.builder(GraphTopology.class).build();
    }

    /**
     * Initialization of the Graph Model Server. This method is called through the blueprint.
     */
    public void init() {
        initTransactionChain();
        initOperationalGraphModel();
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for Graph Model Server {}", this);
        Preconditions.checkState(this.chain == null, "Transaction chain has to be closed before being initialized");
        this.chain = this.dataBroker.createMergingTransactionChain(this);
    }

    /**
     * Initialize GraphModel tree at Data Store top-level.
     */
    private synchronized void initOperationalGraphModel() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        LOG.info("Create Graph Model at top level in Operational DataStore: {}", this.graphTopologyIdentifier);
        trans.put(LogicalDatastoreType.OPERATIONAL, this.graphTopologyIdentifier,
                new GraphTopologyBuilder().setGraph(Collections.emptyList()).build());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize GraphModel {} (transaction {}) by listener {}",
                        ConnectedGraphServer.this.graphTopologyIdentifier, trans.getIdentifier(),
                        ConnectedGraphServer.this, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided.
     */
    private synchronized FluentFuture<? extends CommitInfo> destroyOperationalGraphModel() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, this.graphTopologyIdentifier);
        trans.delete(LogicalDatastoreType.CONFIGURATION, this.graphTopologyIdentifier);
        final FluentFuture<? extends CommitInfo> future = trans.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Operational GraphModel removed {}", ConnectedGraphServer.this.graphTopologyIdentifier);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to reset operational GraphModel {} (transaction {})",
                        ConnectedGraphServer.this.graphTopologyIdentifier, trans.getIdentifier(), throwable);
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
        if (this.chain != null) {
            LOG.debug("Destroy transaction chain for GraphModel {}", this);
            this.chain = null;
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
     * Remove the Operation Graph Model and destroy the transaction chain.
     */
    public void close() {
        destroyOperationalGraphModel();
        destroyTransactionChain();
    }

    /**
     *  DataStore Instance Identifier creation for the various Graph components.
     */
    private InstanceIdentifier<Graph> getGraphInstanceIdentifier(String name) {
        GraphKey graphKey = new GraphKey(name);
        return this.graphTopologyIdentifier.child(Graph.class, graphKey);
    }

    private InstanceIdentifier<Vertex> getVertexInstanceIdentifier(Graph graph, final Vertex vertex) {
        return this.graphTopologyIdentifier.child(Graph.class, graph.key()).child(Vertex.class, vertex.key());
    }

    private InstanceIdentifier<Edge> getEdgeInstanceIdentifier(Graph graph, final Edge edge) {
        return this.graphTopologyIdentifier.child(Graph.class, graph.key()).child(Edge.class, edge.key());
    }

    private InstanceIdentifier<Prefix> getPrefixInstanceIdentifier(Graph graph, final Prefix prefix) {
        return this.graphTopologyIdentifier.child(Graph.class, graph.key()).child(Prefix.class, prefix.key());
    }

    /**
     * Add Graph or Graph components to the Data Store.
     *
     * @param <T>   As a generic method, T must be a Graph, Vertex, Edge or Prefix.
     * @param id    Instance Identifier of the Data Object
     * @param data  Data Object (Graph, Vertex, Edge or Prefix)
     * @param info  Information to be logged
     */
    private synchronized <T extends DataObject> void addToDataStore(final InstanceIdentifier<T> id, final T data,
            final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, id, data);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("GraphModel: {} has been published in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GrahModel: Cannot write {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
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
    private synchronized <T extends DataObject> void updateToDataStore(final InstanceIdentifier<T> id, final T data,
            final InstanceIdentifier<T> old, final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        if (old != null) {
            trans.delete(LogicalDatastoreType.OPERATIONAL, old);
        }
        trans.put(LogicalDatastoreType.OPERATIONAL, id, data);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("GraphModel: {} has been published in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GrahModel: Cannot write {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
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
    private synchronized <T extends DataObject> void removeFromDataStore(final InstanceIdentifier<T> id,
            final String info) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, id);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("GraphModel: {} has been deleted in operational datastore ", info);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("GraphModel: Cannot delete {} to the operational datastore (transaction: {})", info,
                        trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Clear Graph. This method is used by the Connected Graph to clear associated Graph.
     *
     * @param graph Graph associated to the Connected Graph
     */
    public void clearGraph(Graph graph) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        removeFromDataStore(getGraphInstanceIdentifier(graph.getName()), "Graph(" + graph.getName() + ")");
    }

    /**
     * Add Vertex to existing Graph. Old vertex is remove first. This method is called when a Connected Vertex is
     * created (See addVertex() method from ConnectedGraph Interface).
     *
     * @param graph   Graph where the vertex will be stored
     * @param vertex  Vertex to be inserted in the graph
     * @param old     Old vertex when performing an update. Must be null for a simple addition
     */
    public void addVertex(Graph graph, Vertex vertex, Vertex old) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(vertex != null, "Provided Vertex is a null object");
        InstanceIdentifier<Vertex> oldId = null;
        /* Remove old Vertex if it exists before storing the new Vertex */
        if (old != null) {
            oldId = getVertexInstanceIdentifier(graph, old);
        }
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
    public void deleteVertex(Graph graph, Vertex vertex) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(vertex != null, "Provided Vertex is a null object");
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
    public void addEdge(Graph graph, Edge edge, Edge old) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(edge != null, "Provided Edge is a null object");
        InstanceIdentifier<Edge> oldId = null;
        /* Remove old Edge if it exists before storing the new Edge */
        if (old != null) {
            oldId = getEdgeInstanceIdentifier(graph, old);
        }
        updateToDataStore(getEdgeInstanceIdentifier(graph, edge), edge, oldId, "Edge(" + edge.getName() + ")");
    }

    /**
     * Remove Edge to existing Graph. This method is called when a Connected Edge is removed (See deleteEdge()
     * method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the edge is stored
     * @param edge   Edge to be removed
     */
    public void deleteEdge(Graph graph, Edge edge) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(edge != null, "Provided Edge is a null object");
        removeFromDataStore(getEdgeInstanceIdentifier(graph, edge), "Edge(" + edge.getName() + ")");
    }

    /**
     * Add Prefix to existing Graph. This method is called when a Prefix is added to a Connected Vertex
     * (See addPrefix() method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the prefix will be stored
     * @param prefix Prefix to be interted in the graph
     */
    public void addPrefix(Graph graph, Prefix prefix) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(prefix != null, "Provided Prefix is a null object");
        addToDataStore(getPrefixInstanceIdentifier(graph, prefix), prefix, "Prefix(" + prefix.getPrefix() + ")");
    }

    /**
     * Remove Prefix to existing Graph. This method is called when a Prefix is removed from a Connected Vertex
     * (See deletePrefix() method from ConnectedGraph Interface).
     *
     * @param graph  Graph where the prefix is stored
     * @param prefix Prefix to be removed
     */
    public void deletePrefix(Graph graph, Prefix prefix) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        Preconditions.checkArgument(prefix != null, "Provided Prefix is a null object");
        removeFromDataStore(getPrefixInstanceIdentifier(graph, prefix), "Prefix(" + prefix.getPrefix() + ")");
    }

    @Override
    public final synchronized void onTransactionChainFailed(final TransactionChain transactionChain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("GraphModel builder for {} failed in transaction: {} ", this.graphTopologyIdentifier,
                transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain transactionChain) {
        LOG.info("GraphModel builder for {} shut down", this.graphTopologyIdentifier);
    }

    @Override
    public ArrayList<ConnectedGraph> getConnectedGraphs() {
        return new ArrayList<ConnectedGraph>(this.graphs.values());
    }

    @Override
    public ConnectedGraph getConnectedGraph(GraphKey key) {
        return graphs.get(key);
    }

    @Override
    public ConnectedGraph getConnectedGraph(String name) {
        return graphs.get(new GraphKey(name));
    }

    @Override
    public Graph getGraph(GraphKey key) {
        if (graphs.containsKey(key)) {
            return graphs.get(key).getGraph();
        } else {
            return null;
        }
    }

    @Override
    public Graph getGraph(String name) {
        return getGraph(new GraphKey(name));
    }

    @Override
    public ConnectedGraph createConnectedGraph(String name, DomainScope scope) {
        Graph graph = new GraphBuilder()
                .setName(name)
                .setDomainScope(scope)
                .setEdge(Collections.emptyList())
                .setVertex(Collections.emptyList())
                .setPrefix(Collections.emptyList())
                .build();
        addToDataStore(getGraphInstanceIdentifier(name), graph, "Graph(" + name + ")");
        ConnectedGraphImpl cgraph = new ConnectedGraphImpl(graph, this);
        graphs.put(graph.key(), cgraph);
        return cgraph;
    }

    @Override
    public ConnectedGraph addGraph(Graph graph) {
        Preconditions.checkArgument(graph != null, "Provided Graph is a null object");
        addToDataStore(getGraphInstanceIdentifier(graph.getName()), graph, "Graph(" + graph.getName() + ")");
        ConnectedGraphImpl cgraph = new ConnectedGraphImpl(graph, this);
        graphs.put(graph.key(), cgraph);
        return cgraph;
    }

    @Override
    public void deleteGraph(GraphKey key) {
        Preconditions.checkArgument(key != null, "Provided Graph Key is a null object");
        ConnectedGraphImpl cgraph = graphs.remove(key);
        /*
         * Remove the corresponding Connected Graph which will delete the graph
         * by calling clearGraph() method (see above)
         */
        if (cgraph != null) {
            cgraph.clear();
        }
    }
}
