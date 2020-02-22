/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;

/**
 * This Class implements the Connected Edge used by the Connected Graph for path computation algorithms.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */

public class ConnectedEdgeImpl implements ConnectedEdge {

    /* Reference to Source and Destination Connected Vertex within the Connected Graph */
    private ConnectedVertexImpl source;
    private ConnectedVertexImpl destination;

    /* Reference to the Edge within the Graph associated to the Connected Graph */
    private Edge edge;

    /* Edge key in the Connected Graph */
    private Long ceid;

    public ConnectedEdgeImpl(@NonNull Long key) {
        checkArgument(key != 0, "Edge Key must not be equal to 0");
        this.ceid = key;
        this.edge = null;
    }

    public ConnectedEdgeImpl(@NonNull Edge edge) {
        checkArgument(edge.getEdgeId().longValue() != 0, "Edge Key must not be equal to 0");
        this.edge = edge;
        this.ceid = edge.getEdgeId().longValue();
    }

    /**
     * When edge is removed, we must disconnect source and destination Connected Vertices.
     */
    void close() {
        this.disconnect();
    }

    /**
     * Set Connected Vertex as source.
     *
     * @param vertex Vertex
     */
    public ConnectedEdgeImpl setSource(ConnectedVertexImpl vertex) {
        source = vertex;
        return this;
    }

    /**
     * Set Connected Vertex as destination.
     *
     * @param vertex Vertex
     */
    public ConnectedEdgeImpl setDestination(ConnectedVertexImpl vertex) {
        destination = vertex;
        return this;
    }

    /**
     * Disconnect source Connected Vertex.
     */
    public void disconnectSource() {
        if (source != null) {
            source.removeOutput(this);
            source = null;
        }
    }

    /**
     * Disconnect destination Connected Vertex.
     */
    public void disconnectDestination() {
        if (destination != null) {
            destination.removeInput(this);
            destination = null;
        }
    }

    /**
     * Disconnect both source and destination Connected Vertices.
     */
    public void disconnect() {
        disconnectSource();
        disconnectDestination();
    }

    /**
     * Set associated Edge to this Connected Edge.
     *
     * @param edge Edge
     */
    public ConnectedEdgeImpl setEdge(Edge edge) {
        this.edge = edge;
        return this;
    }

    @Override
    public @NonNull Long getKey() {
        return this.ceid;
    }

    @Override
    public ConnectedVertex getSource() {
        return this.source;
    }

    @Override
    public ConnectedVertex getDestination() {
        return this.destination;
    }

    @Override
    public Edge getEdge() {
        return this.edge;
    }

    /**
     * Returns the name of the associated Edge if set or the interface address otherwise.
     *
     * @return Edge name or interface address
     */
    @Override
    public String toString() {
        if (edge == null) {
            return "Null";
        }
        if (edge.getName() != null) {
            return edge.getName();
        }
        if (edge.getEdgeAttributes() != null) {
            if (edge.getEdgeAttributes().getLocalAddress() != null) {
                return edge.getEdgeAttributes().getLocalAddress().toString();
            }
            if (edge.getEdgeAttributes().getLocalIdentifier() != null) {
                return edge.getEdgeAttributes().getLocalIdentifier().toString();
            }
        }
        return "Unknown Edge";
    }
}
