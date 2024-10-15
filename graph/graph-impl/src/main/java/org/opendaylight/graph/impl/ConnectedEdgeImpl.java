/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedEdgeTrigger;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Edge;

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
    private final Long ceid;

    /* Total amount of Bandwidth reserved by Constrained Paths */
    private static final int MAX_PRIORITY = 8;
    private Long globalResvBandwidth = 0L;
    private final Long[] cosResvBandwidth = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};

    /* List of Connected Edge Trigger */
    private final ConcurrentMap<String, ConnectedEdgeTrigger> triggers = new ConcurrentHashMap<>();

    public ConnectedEdgeImpl(final @NonNull Long key) {
        checkArgument(key != 0, "Edge Key must not be equal to 0");
        ceid = key;
        edge = null;
    }

    public ConnectedEdgeImpl(final @NonNull Edge edge) {
        checkArgument(edge.getEdgeId().longValue() != 0, "Edge Key must not be equal to 0");
        this.edge = edge;
        ceid = edge.getEdgeId().longValue();
    }

    /**
     * When edge is removed, we must disconnect source and destination Connected Vertices.
     */
    void close() {
        triggers.clear();
        disconnect();
    }

    /**
     * Set Connected Vertex as source.
     *
     * @param vertex Vertex
     */
    public ConnectedEdgeImpl setSource(final ConnectedVertexImpl vertex) {
        source = vertex;
        return this;
    }

    /**
     * Set Connected Vertex as destination.
     *
     * @param vertex Vertex
     */
    public ConnectedEdgeImpl setDestination(final ConnectedVertexImpl vertex) {
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
    public ConnectedEdgeImpl setEdge(final Edge edge) {
        this.edge = edge;
        return this;
    }

    @Override
    public @NonNull Long getKey() {
        return ceid;
    }

    @Override
    public ConnectedVertex getSource() {
        return source;
    }

    @Override
    public ConnectedVertex getDestination() {
        return destination;
    }

    @Override
    public Edge getEdge() {
        return edge;
    }

    @Override
    public Long getGlobalResvBandwidth() {
        return globalResvBandwidth;
    }

    @Override
    public Long getCosResvBandwidth(final int cos) {
        if (cos < 0 || cos > MAX_PRIORITY) {
            return null;
        } else {
            return cosResvBandwidth[cos];
        }
    }

    @Override
    public void addBandwidth(final Long bw, final int cos) {
        if (cos < 0 || cos > MAX_PRIORITY) {
            return;
        }
        globalResvBandwidth += bw;
        cosResvBandwidth[cos] += bw;
    }

    @Override
    public void delBandwidth(final Long bw, final int cos) {
        if (cos < 0 || cos > MAX_PRIORITY) {
            return;
        }
        globalResvBandwidth -= bw;
        if (globalResvBandwidth < 0) {
            globalResvBandwidth = 0L;
        }
        cosResvBandwidth[cos] -= bw;
        if (cosResvBandwidth[cos] < 0) {
            cosResvBandwidth[cos] = 0L;
        }
    }

    @Override
    public boolean registerTrigger(final ConnectedEdgeTrigger trigger, final String key) {
        return triggers.putIfAbsent(key, trigger) == null;
    }

    @Override
    public boolean unRegisterTrigger(final ConnectedEdgeTrigger trigger, final String key) {
        return triggers.remove(key, trigger);
    }

    public List<ConnectedEdgeTrigger> getTriggers() {
        return new ArrayList<>(triggers.values());
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
