/*
 * Copyright (c) 2019 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.GraphKey;

/**
 * Connected Graph Provider is a new service provided by the Graph feature to manage the Connected Graph.
 *
 * <p>
 * It allows to get, create, add and delete Connected Graph. All associated Graph are automatically
 * stored in the DataStore.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 *
 */

public interface ConnectedGraphProvider {

    /**
     * Returns Graph for the given graph name.
     *
     * @param name Name of the Graph
     *
     * @return Graph
     */
    Graph getGraph(String name);

    /**
     * Returns the Graph for the given graph key.
     *
     * @param key Unique Graph Identifier
     *
     * @return Graph
     */
    Graph getGraph(GraphKey key);

    /**
     * Returns Connected Graph for the given graph name.
     *
     * @param name Name of the Graph
     *
     * @return Connected Graph
     */
    ConnectedGraph getConnectedGraph(String name);

    /**
     * Returns Connected Graph for the given graph key.
     *
     * @param key Unique Graph Identifier
     *
     * @return Connected Graph
     */
    ConnectedGraph getConnectedGraph(GraphKey key);

    /**
     * Returns all registered Connected Graphs.
     *
     * @return List of Connected Graph
     */
    List<ConnectedGraph> getConnectedGraphs();

    /**
     * Create Connected Graph and associated Graph for given name and Graph Type. The associated Graph is also stored
     * in the DataStore.
     *
     * @param name  Name of the Graph
     * @param scope Domain scope of the Graph (Intra or Inter domain)
     *
     * @return Connected Graph
     */
    ConnectedGraph createConnectedGraph(String name, DomainScope scope);

    /**
     * Add a Graph. This action will automatically create the associated Connected Graph and store is in the DataStore.
     *
     * @param graph  Graph to be added
     *
     * @return Connected Graph
     */
    ConnectedGraph addGraph(Graph graph);

    /**
     * Remove a Graph. This action will automatically delete the associated Connected Graph and store is in the
     * DataStore.
     *
     * @param key  Graph Identifier
     *
     */
    void deleteGraph(GraphKey key);

}
