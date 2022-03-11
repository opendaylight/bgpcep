/*
 * Copyright (c) 2019 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;

/**
 * Connected Graph class is the connected version of the Graph class from the graph yang model.
 *
 * <p>
 * Connected Graph is composed by Connected Vertex, Connected Edges and Prefix. It models Unidirectional Connected
 * Graph (see graph theory). So, Edge and Connected Edge are unidirectional, thus to connect bi-directionally 2 vertices
 * Va and Vb, it is necessary to setup 2 edges: Va to Vb and Vb to Va.
 * <pre>
 * {@code
 *        --------------     ---------------------------    --------------
 *        | Connected  |---->| Connected Edge Va to Vb |--->| Connected  |
 *   ---->|  Vertex    |     ---------------------------    |  Vertex    |---->
 *        |            |                                    |            |
 *        | - Key (Va) |                                    | - Key (Vb) |
 *   <----| - Vertex   |     ---------------------------    | - Vertex   |<----
 *        |            |<----| Connected Edge Vb to Va |<---|            |
 *        --------------     ---------------------------    --------------
 * }
 * </pre>
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
public interface ConnectedGraph {

    /**
     * Returns the Graph associated to this Connected Graph.
     *
     * @return Graph
     */
    Graph getGraph();

    /**
     * Returns the list of Connected Vertices that form this graph.
     *
     * @return list of Connected Vertices
     */
    List<ConnectedVertex> getVertices();

    /**
     * Returns the Vertex associated to the given key.
     *
     * @param key Unique Vertex Identifier
     * @return Vertex or null if there is no Vertex associated to the given key in this graph
     */
    ConnectedVertex getConnectedVertex(@NonNull Long key);

    /**
     * Returns the Vertex associated to the given IP address.
     *
     * @param address IP address of the Loopback of the Vertex
     *
     * @return Vertex or null if there is no Vertex associated to the given IP address in this graph
     */
    ConnectedVertex getConnectedVertex(IpAddress address);

    /**
     * Add Vertex in the Connected Graph. This action will automatically create the associated Connected Vertex and
     * update the Graph in the DataStore.
     *
     * @param vertex Vertex to be added
     *
     * @return Connected Vertex associated to the given Vertex
     */
    ConnectedVertex addVertex(Vertex vertex);

    /**
     * Remove the Vertex in the Connected Graph. This action will automatically remove the associated Connected Vertex
     * and update the Graph in the DataStore.
     *
     * @param vertexKey Unique Vertex Identifier
     */
    void deleteVertex(VertexKey vertexKey);

    /**
     * Returns the number of Vertices in the graph.
     *
     * @return number of vertices
     */
    int getVerticesSize();

    /**
     * Returns the list of Connected Edges that form this graph.
     *
     * @return list of Connected Edges
     */
    List<ConnectedEdge> getEdges();

    /**
     * Returns the Edge associated to the given key.
     *
     * @param key Unique Edge Identifier
     * @return Edge or null if there is no Edge associated to the given key in this graph
     */
    ConnectedEdge getConnectedEdge(@NonNull Long key);

    /**
     * Returns the Edge associated to the given IP address.
     *
     * @param address IP address of the Edge
     *
     * @return Edge or null if there is no Edge associated to the given IP address in this graph
     */
    ConnectedEdge getConnectedEdge(IpAddress address);

    /**
     * Add Edge in the Connected Graph. This action will automatically create the associated Connected Edge and
     * update the Graph in the DataStore.
     *
     * @param edge Edge to be added
     *
     * @return Connected Edge associated to the given Edge
     */
    ConnectedEdge addEdge(Edge edge);

    /**
     * Remove the Edge in the Connected Graph. This action will automatically remove the associated Connected Edge
     * and update the Graph in the DataStore.
     *
     * @param edgeKey Unique Edge Identifier
     */
    void deleteEdge(EdgeKey edgeKey);

    /**
     * Returns the number of Edges in the graph.
     *
     * @return number of edges
     */
    int getEdgesSize();

    /**
     * Returns the list of Prefix that are stored in this graph.
     *
     * @return list of Prefix
     */
    List<Prefix> getPrefixes();

    /**
     * Returns the Prefix associated to the given IP prefix.
     *
     * @param ippfx IPv4 or IPv6 prefix
     *
     * @return Prefix that match the given IPv4 or IPv6 prefix
     */
    Prefix getPrefix(IpPrefix ippfx);

    /**
     * Add Prefix in the Connected Graph. This action will automatically update the Graph in the DataStore.
     *
     * @param prefix Prefix to be added
     *
     */
    void addPrefix(Prefix prefix);

    /**
     * Remove the Prefix in the Connected Graph. This action will automatically update the Graph in the DataStore.
     *
     * @param ippfx IPv4 or IPv6 prefix
     */
    void deletePrefix(IpPrefix ippfx);

    /**
     * Clear the Connected Graph by removing all Vertices, Edges and Prefixes. This also remove the associated Graph
     * in the Datastore.
     *
     */
    void clear();

    /**
     * Returns the summary of the graph characteristics: number of Vertices, Edges and Prefix.
     *
     * @return characteristics of the Graph as a string
     */
    String getSummary();

    /**
     * Register a trigger that is executed when a problem occurs on a Vertex or a Edge within the Connected Graph.
     *
     * @param trigger   Trigger to be registered
     * @param key       Topology Key Identifier
     *
     * @return          True if registration is done, false otherwise
     */
    boolean registerTrigger(ConnectedGraphTrigger trigger, TopologyKey key);

    /**
     * Un-register a trigger that is already registered on the Connected Graph.
     *
     * @param trigger   Trigger to be unregistered
     * @param key       Topology Key Identifier
     *
     * @return          True if un-registration is done, false otherwise
     */
    boolean unRegisterTrigger(ConnectedGraphTrigger trigger, TopologyKey key);
}
