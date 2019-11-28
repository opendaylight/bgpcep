/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.GraphTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods required to manage the network
 *  representation elements in the datastore.
 *
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */

public class GraphListener implements DataTreeChangeListener<Graph> {

    private static final Logger LOG = LoggerFactory.getLogger(GraphListener.class);
    private final DataBroker dataBroker;
    private ListenerRegistration<GraphListener> listenerRegistration;
    private final InstanceIdentifier<Graph> graphIdentifier;
    private final ConnectedGraphProvider graphProvider;

    public GraphListener(final DataBroker dataBroker, final ConnectedGraphProvider provider) {
        this.dataBroker = dataBroker;
        this.graphIdentifier = InstanceIdentifier.builder(GraphTopology.class).child(Graph.class).build();
        this.graphProvider = provider;
        LOG.info("Graph Model Listener started");
    }

    /**
     * Initialization of the Graph Topology Listener. This method is called through the blueprint.
     */
    public void init() {
        Preconditions.checkState(this.listenerRegistration == null, "Graph Listener has been registered before");
        final DataTreeIdentifier<Graph> treeId = DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                graphIdentifier);
        this.listenerRegistration = this.dataBroker.registerDataTreeChangeListener(treeId, this);
        LOG.info("Registered listener {} on Graph Model at {}", this, this.graphIdentifier);
    }

    /**
     * Unregister to data tree change listener.
     */
    private synchronized void unregisterDataChangeListener() {
        if (this.listenerRegistration != null) {
            LOG.debug("Unregistered listener {} on Graph", this);
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    /**
     * Close this Listener.
     */
    public void close() {
        unregisterDataChangeListener();
    }

    /**
     * Parse Sub Tree modification. This method is called with the Modified Children from
     * the Data Tree Modification root.This method is necessary as the getModificationType() method returns
     * SUBTREE_MODIFIED only when Data Object is already present in the Data Store. Thus, this is indication is only
     * relevant for deletion not for insertion where WRITE modification type is return even if it concerns a child.
     *
     * @param cgraph   Connected Graph where children Data Object must insert or remove
     * @param children List of children (Vertex, Edge or Prefix)
     */
    private void parseSubTree(ConnectedGraph cgraph,
            Collection<? extends DataObjectModification<? extends DataObject>> children) {
        for (DataObjectModification<? extends DataObject> child : children) {
            DataObject value;
            switch (child.getModificationType()) {
                case DELETE:
                    value = child.getDataBefore();
                    if (value instanceof Vertex) {
                        cgraph.deleteVertex(((Vertex )value).key());
                    }
                    if (value instanceof Edge) {
                        cgraph.deleteEdge(((Edge )value).key());
                    }
                    if (value instanceof Prefix) {
                        cgraph.deletePrefix(((Prefix )value).getPrefix());
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    value = child.getDataAfter();
                    if (value instanceof Vertex) {
                        cgraph.addVertex((Vertex )value);
                    }
                    if (value instanceof Edge) {
                        cgraph.addEdge((Edge )value);
                    }
                    if (value instanceof Prefix) {
                        cgraph.addPrefix((Prefix )value);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Graph>> changes) {
        for (DataTreeModification<Graph> change : changes) {
            DataObjectModification<Graph> root = change.getRootNode();
            GraphKey key = change.getRootPath().getRootIdentifier().firstKeyOf(Graph.class);
            switch (root.getModificationType()) {
                case DELETE:
                    graphProvider.deleteGraph(key);
                    break;
                case SUBTREE_MODIFIED:
                    /* getModificationType() returns SUBTREE_MODIFIED only when Data Object is already present in the
                     * Data Store, thus, only for deletion. Thus, to insert children, we must used parseSubTree()
                     * method (See above). This method is called only when the graph already exists.
                     */
                case WRITE:
                    /* First look if the Graph was not already configured */
                    ConnectedGraph cgraph = this.graphProvider.getConnectedGraph(key);
                    if (cgraph == null) {
                        graphProvider.addGraph(root.getDataAfter());
                    } else {
                        /* Graph exist, process Children */
                        parseSubTree(cgraph, root.getModifiedChildren());
                    }
                    break;
                default:
                    break;
            }
        }
    }

}

