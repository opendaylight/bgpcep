/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.GraphTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Vertex;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class Implements the DataStoreService interface providing the methods required to manage the network
 * representation elements in the Data Store.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
@Component(service = {})
@Singleton
public final class GraphListener implements DataTreeChangeListener<Graph>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraphListener.class);
    private static final InstanceIdentifier<Graph> GRAPH_IDENTIFIER =
        InstanceIdentifier.builder(GraphTopology.class).child(Graph.class).build();

    private final ConnectedGraphProvider graphProvider;

    private ListenerRegistration<GraphListener> listenerRegistration;

    @Inject
    @Activate
    public GraphListener(@Reference final DataBroker dataBroker, @Reference final ConnectedGraphProvider provider) {
        this.graphProvider = requireNonNull(provider);
        this.listenerRegistration = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, GRAPH_IDENTIFIER), this);
        LOG.info("Registered listener {} on Graph Model at {}", this, GRAPH_IDENTIFIER);
    }

    /**
     * Close this Listener.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        if (this.listenerRegistration != null) {
            LOG.debug("Unregistered listener {} on Graph", this);
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
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
    private void parseSubTree(final ConnectedGraph cgraph,
            final Collection<? extends DataObjectModification<? extends DataObject>> children) {
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

