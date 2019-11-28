/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;


/**
 * This Class implements the Connected Vertex used by the Connected Graph for path computation algorithms.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */

public class ConnectedVertexImpl implements ConnectedVertex {

    /* Reference to input and output Connected Edge within the Connected Graph */
    private ArrayList<ConnectedEdgeImpl> input = new ArrayList<>();
    private ArrayList<ConnectedEdgeImpl> output = new ArrayList<>();

    /* List of Prefixes announced by this Vertex */
    private ArrayList<Prefix> prefixes = new ArrayList<>();

    /* Reference to the Vertex of the standard Graph associated to the Connected Graph */
    private Vertex vertex = null;

    /* Connected Vertex Identifier */
    private long cvid;

    public ConnectedVertexImpl(long key) {
        this.cvid = key;
        this.vertex = null;
    }

    public ConnectedVertexImpl(Vertex vertex) {
        this.cvid = vertex.getVertexId().longValue();
        this.vertex = vertex;
    }

    /**
     * When vertex is removed, we must disconnect all Connected Edges.
     */
    void close() {
        this.disconnect();
    }

    /**
     * Set associated Vertex to this Connected Vertex.
     *
     * @param vertex Vertex
     */
    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    /**
     * Add Connected Edge as input edge.
     *
     * @param edge Connected Edge
     */
    public void addInput(ConnectedEdgeImpl edge) {
        if (!input.contains(edge)) {
            input.add(edge);
        }
    }

    /**
     * Add Connected Edge as output edge.
     *
     * @param edge Connected Edge
     */
    public void addOutput(ConnectedEdgeImpl edge) {
        if (!output.contains(edge)) {
            output.add(edge);
        }
    }

    /**
     * Remove input Connected Edge.
     *
     * @param edge Connected Edge
     */
    public void removeInput(ConnectedEdgeImpl edge) {
        input.remove(edge);
    }

    /**
     * Remove output Connected Edge.
     *
     * @param edge Connected Edge
     */
    public void removeOutput(ConnectedEdgeImpl edge) {
        output.remove(edge);
    }

    /**
     * Disconnect all input and output Connected Edge.
     */
    public void disconnect() {
        for (ConnectedEdgeImpl edge : input) {
            edge.disconnectDestination();
        }
        for (ConnectedEdgeImpl edge : output) {
            edge.disconnectSource();
        }
    }

    /**
     * Add Prefix to this Connected Vertex.
     *
     * @param prefix Prefix
     */
    public void addPrefix(Prefix prefix) {
        if (!prefixes.contains(prefix)) {
            prefixes.add(prefix);
        }
    }

    /**
     * Remove Prefix.
     *
     * @param prefix Prefix
     */
    public void removePrefix(Prefix prefix) {
        if (prefixes.contains(prefix)) {
            prefixes.remove(prefix);
        }
    }


    @Override
    public long getKey() {
        return this.cvid;
    }

    @Override
    public Vertex getVertex() {
        return this.vertex;
    }

    @Override
    public ConnectedEdge getEdgeTo(long dstRid) {
        for (ConnectedEdge edge : output) {
            if (edge.getDestination().getKey() == dstRid) {
                return edge;
            }
        }
        return null;
    }

    @Override
    public ArrayList<Edge> getInputEdges() {
        ArrayList<Edge> edgeList = new ArrayList<Edge>();
        for (ConnectedEdge edge : input) {
            edgeList.add(edge.getEdge());
        }
        return edgeList;
    }

    @Override
    public ArrayList<ConnectedEdge> getInputEdges(long minBw, int pri) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if (pri < 0 || pri > 7) {
            return liste;
        }
        for (ConnectedEdge edge : input) {
            List<BigDecimal> unRsvBw = edge.getEdge().getEdgeAttributes().getUnreservedBandwidth();
            if (unRsvBw.get(pri).longValue() >= minBw) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public ArrayList<ConnectedEdge> getInputConnectedEdges() {
        return new ArrayList<ConnectedEdge>(this.input);
    }

    @Override
    public ArrayList<Edge> getOutputEdges() {
        ArrayList<Edge> edgeList = new ArrayList<Edge>();
        for (ConnectedEdge edge : output) {
            edgeList.add(edge.getEdge());
        }
        return edgeList;
    }

    @Override
    public ArrayList<ConnectedEdge> getOutputEdges(long minBw, int pri) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if (pri < 0 || pri > 7) {
            return liste;
        }
        for (ConnectedEdge edge : output) {
            EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();
            List<BigDecimal> unRsvBw = attributes.getUnreservedBandwidth();
            if (unRsvBw.get(pri).longValue() >= minBw
                    && attributes.getMaxLinkBandwidth().longValue() >= minBw
                    && attributes.getMaxResvLinkBandwidth().longValue() >= minBw) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public ArrayList<ConnectedEdge> getOutputEdges(long minBw, int pri, int delay, int te) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if (pri < 0 || pri > 7) {
            return liste;
        }
        for (ConnectedEdge edge : output) {
            EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();
            List<BigDecimal> unRsvBw = attributes.getUnreservedBandwidth();
            if (unRsvBw.get(pri).longValue() >= minBw
                    && attributes.getMaxLinkBandwidth().longValue() >= minBw
                    && attributes.getMaxResvLinkBandwidth().longValue() >= minBw
                    && (te != 0 || attributes.getTeMetric().intValue() <= te)
                    && (delay != 0 || attributes.getDelay().getValue().intValue() <= delay)) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public ArrayList<ConnectedEdge> getOutputConnectedEdges() {
        return new ArrayList<ConnectedEdge>(this.output);
    }

    @Override
    public ArrayList<Prefix> getPrefixes() {
        return this.prefixes;
    }

    @Override
    public String toString() {
        if (vertex.getName() != null) {
            return vertex.getName();
        } else {
            return vertex.getRouterId().toString();
        }
    }
}
