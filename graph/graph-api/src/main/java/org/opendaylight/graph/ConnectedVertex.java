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
     * Returns the list of incoming Connected Edge that has at least the specified minimum Bandwidth available for
     * the given Class Type.
     *
     * <p>This method check the Unreserved Bandwidth value for the given Class type from the Attributes of Edge Class
     * from all input Connected Edges. Only Connected Edges that meet the constraint are returned.</p>
     *
     * @param minimumBandwidth Minimum Bandwidth (units byte/sec)
     * @param classType Class Type (i.e. 0..7)
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getInputEdges(long minimumBandwidth, int classType);

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
     * Returns the list of outgoing Connected Edge that has at least the specified minimum Bandwidth available for
     * the given Class Type.
     *
     * <p>This method check the Unreserved Bandwidth value for the given Class type from the Attributes of Edge Class
     * from all output Connected Edges. Only Connected Edges that meet the constraint are returned.</p>
     *
     * @param minimumBandwidth Minimum Bandwidth (units byte/sec)
     * @param classType Class Type (i.e. 0..7)
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getOutputEdges(long minimumBandwidth, int classType);

    /**
     * Returns the list of outgoing Connected Edge that has at least the specified minimum Bandwidth available for
     * the given Class Type, a delay less that the specified delay and a TE metric less than the specified one.
     *
     * <p>This method check the Unreserved Bandwidth value for the given Class type, the delay and the TE metric from
     * the Attributes of Edge Class from all output Connected Edges.
     * Only Connected Edges that meet the constraint are returned.</p>
     *
     * @param minimumBandwidth Minimum Bandwidth (units byte/sec)
     * @param classType Class Type (i.e. 0..7)
     * @param delay Bounded Delay (units milliseconds)
     * @param teMetric Traffic Engineering Metric
     *
     * @return List of Connected Edge
     */
    List<ConnectedEdge> getOutputEdges(long minimumBandwidth, int classType, int delay, int teMetric);

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
     * Return the name of the associated Vertex if set or the router-id otherwise.
     *
     * @return Vertex name or router-id
     */
    String toString();
}
