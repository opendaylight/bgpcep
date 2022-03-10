/*
 * Copyright (c) 2019 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;

/**
 * Connected Edge class is the connected version of the Edge class from the graph yang model.
 *
 * <p>
 * It is composed of a reference to the associated Edge class from the Graph class,
 * a unique Key identifier in the associated Connected Graph,
 * and two references to the associated Connected Vertex in the connected Graph: source and destination.
 * <pre>
 * {@code
 * ---------------------------                        --------------------------------
 * | Source Connected Vertex |---- Connected Edge --->| Destination Connected Vertex |
 * ---------------------------                        --------------------------------
 * }
 * </pre>
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
public interface ConnectedEdge {
    /**
     * Returns unique key associated to this Connected Edge.
     *
     * @return Edge Key
     */
    @NonNull Long getKey();

    /**
     * Returns the Edge from the Graph associated to this connection.
     *
     * @return Edge associated to this connection
     */
    @NonNull Edge getEdge();

    /**
     * Returns the source Connected Vertex from the Connected Graph associated to this Connected Edge.
     *
     * @return Source Connected Vertex
     */
    @Nullable ConnectedVertex getSource();

    /**
     * Returns the destination Connected Vertex from the Connected Graph associated to this Connected Edge.
     *
     * @return Destination Connected Vertex
     */
    @Nullable ConnectedVertex getDestination();

    /**
     * Returns the total amount of Bandwidth consumes by all Constrained Paths that belong to the Edge associated
     * to this Connected Edge for all Class of Service.
     *
     * @return      Global Reserved Bandwidth
     */
    Long getGlobalResvBandwidth();

    /**
     * Returns the total amount of Bandwidth consumes by all Constrained Paths that belong to the Edge associated
     * to this Connected Edge for the given Class of Service (CoS).
     *
     * @param cos   Class of Service
     *
     * @return      Reserved Bandwidth per CoS
     */
    Long getCosResvBandwidth(int cos);

    /**
     * Add the given Bandwidth for the given Class of Service (CoS) to the associated Edge of this Connected Edge.
     * This method increments by the Bandwidth value the GlobalResvBandwidth and the ReservedBandwidth[cos] attributes.
     *
     * @param bw    Bandwidth consumed by the TE Path
     * @param cos   Class of Service of the TE Path
     */
    void addBandwidth(Long bw, int cos);

    /**
     * Remove the given Bandwidth for the given Class of Service (CoS) to the associated Edge of this Connected Edge.
     * This method decrements by the Bandwidth value the GlobalResvBandwidth and the ReservedBandwidth[cos] attributes.
     *
     * @param bw    Bandwidth consumed by the TE Path
     * @param cos   Class of Service of the TE Path
     */
    void delBandwidth(Long bw, int cos);
}
