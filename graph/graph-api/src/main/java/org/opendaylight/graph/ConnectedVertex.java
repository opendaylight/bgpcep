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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;

/**
 * Connected Vertex class is the connected version of the Vertex class from the graph yang model.
 *
 * <p>
 * It is composed of a reference to the associated Vertex class from the Graph class,
 * a unique Key identifier in the associated Connected Graph,
 * and two lists to the associated Connected Edges in the connected Graph: input and output.
 * <pre>
 * {@code
 *                              -------------
 *                              | Connected |
 *                         ---->|  Vertex   |---->
 * Input Connected Edges {  ... | - Key     | ...  } Output Connected Edges
 *                         ---->| - Vertex  |---->
 *                              -------------
 * }
 * </pre>
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 *
 */
public interface ConnectedVertex {

    /**
     * Returns unique key associated to this Connected Vertex.
     *
     * @return Vertex Key
     */
    @NonNull Long getKey();

    /**
     * Returns Vertex associated to this Connected Vertex.
     *
     * @return vertex Vertex
     */
    @NonNull Vertex getVertex();

    /**
     * Returns Connected Edges that has for destination the Connected Vertex identified by its key.
     *
     * @param destinationKey Unique Key that identify the destination Vertex
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getEdgeTo(Long destinationKey);

    /**
     * Returns the list of incoming Edge for this Connected Vertex.
     *
     * @return List of Edge
     */
    List<Edge> getInputEdges();

    /**
     * Returns the list of incoming Connected Edge for this Connected Vertex.
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getInputConnectedEdges();

    /**
     * Returns the list of outgoing Edge for this Connected Vertex.
     *
     * @return List of Edge
     */
    List<Edge> getOutputEdges();

    /**
     * Returns the list of outgoing Connected Edge for this Connected Vertex.
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getOutputConnectedEdges();

    /**
     * Return the list of prefix announced by this Connected Vertex. Prefix contains the associated SID when
     * Segment Routing is enable.
     *
     * @return List of Prefix
     */
    List<Prefix> getPrefixes();

    /**
     * Register a trigger that is executed when a problem occurs on the Connected Vertex.
     *
     * @param trigger   Trigger to be registered
     * @param key       A unique key as string e.g. NodeId+LspId
     *
     * @return          True if registration is done, false otherwise
     */
    boolean registerTrigger(ConnectedVertexTrigger trigger, String key);

    /**
     * Un-register a trigger that is already registered on the Connected Vertex.
     *
     * @param key   A unique key as string e.g. NodeId+LspId
     *
     * @return      True if un-registration is done, false otherwise
     */
    boolean unRegisterTrigger(String key);
}
