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
import java.util.List;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.GraphTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Vertex;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
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

    private final ConnectedGraphProvider graphProvider;

    private Registration listenerRegistration;

    @Inject
    @Activate
    public GraphListener(@Reference final DataBroker dataBroker, @Reference final ConnectedGraphProvider provider) {
        graphProvider = requireNonNull(provider);

        final var graphIdentifier = DataObjectReference.builder(GraphTopology.class).child(Graph.class).build();

        listenerRegistration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION,
            graphIdentifier, this);
        LOG.info("Registered listener {} on Graph Model at {}", this, graphIdentifier);
    }

    /**
     * Close this Listener.
     */
    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        if (listenerRegistration != null) {
            LOG.debug("Unregistered listener {} on Graph", this);
            listenerRegistration.close();
            listenerRegistration = null;
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
    private static void parseSubTree(final ConnectedGraph cgraph,
            final Collection<? extends DataObjectModification<?>> children) {
        for (var child : children) {
            final DataObject value;
            switch (child.modificationType()) {
                case DELETE:
                    value = child.dataBefore();
                    if (value instanceof Vertex vertex) {
                        cgraph.deleteVertex(vertex.key());
                    }
                    if (value instanceof Edge edge) {
                        cgraph.deleteEdge(edge.key());
                    }
                    if (value instanceof Prefix prefix) {
                        cgraph.deletePrefix(prefix.getPrefix());
                    }
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    value = child.dataAfter();
                    if (value instanceof Vertex vertex) {
                        cgraph.addVertex(vertex);
                    }
                    if (value instanceof Edge edge) {
                        cgraph.addEdge(edge);
                    }
                    if (value instanceof Prefix prefix) {
                        cgraph.addPrefix(prefix);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<Graph>> changes) {
        for (var change : changes) {
            final var root = change.getRootNode();
            final var key = change.path().firstKeyOf(Graph.class);
            switch (root.modificationType()) {
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
                    ConnectedGraph cgraph = graphProvider.getConnectedGraph(key);
                    if (cgraph == null) {
                        graphProvider.addGraph(root.dataAfter());
                    } else {
                        /* Graph exist, process Children */
                        parseSubTree(cgraph, root.modifiedChildren());
                    }
                    break;
                default:
                    break;
            }
        }
    }
}

