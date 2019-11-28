/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yangtools.yang.common.Uint32;


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
    private Long cvid;

    public ConnectedVertexImpl(@NonNull Long key) {
        checkArgument(key != 0, "Vertex Key must not be equal to 0");
        this.cvid = key;
        this.vertex = null;
    }

    public ConnectedVertexImpl(@NonNull Vertex vertex) {
        checkArgument(vertex.getVertexId().longValue() != 0, "Vertex Key must not be equal to 0");
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
    public ConnectedVertexImpl setVertex(Vertex vertex) {
        this.vertex = vertex;
        return this;
    }

    /**
     * Add Connected Edge as input edge.
     *
     * @param edge Connected Edge
     */
    public ConnectedVertexImpl addInput(ConnectedEdgeImpl edge) {
        if (!input.contains(edge)) {
            input.add(edge);
        }
        return this;
    }

    /**
     * Add Connected Edge as output edge.
     *
     * @param edge Connected Edge
     */
    public ConnectedVertexImpl addOutput(ConnectedEdgeImpl edge) {
        if (!output.contains(edge)) {
            output.add(edge);
        }
        return this;
    }

    /**
     * Remove input Connected Edge.
     *
     * @param edge Connected Edge
     */
    public ConnectedVertexImpl removeInput(ConnectedEdgeImpl edge) {
        input.remove(edge);
        return this;
    }

    /**
     * Remove output Connected Edge.
     *
     * @param edge Connected Edge
     */
    public ConnectedVertexImpl removeOutput(ConnectedEdgeImpl edge) {
        output.remove(edge);
        return this;
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
    public ConnectedVertexImpl addPrefix(Prefix prefix) {
        if (!prefixes.contains(prefix)) {
            prefixes.add(prefix);
        }
        return this;
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
    public Long getKey() {
        return this.cvid;
    }

    @Override
    public Vertex getVertex() {
        return this.vertex;
    }

    @Override
    public List<ConnectedEdge> getEdgeTo(Long dstRid) {
        ArrayList<ConnectedEdge> edgeList = new ArrayList<ConnectedEdge>();
        for (ConnectedEdge edge : output) {
            if (edge.getDestination().getKey().equals(dstRid)) {
                edgeList.add(edge);
            }
        }
        return edgeList;
    }

    @Override
    public List<Edge> getInputEdges() {
        ArrayList<Edge> edgeList = new ArrayList<Edge>();
        for (ConnectedEdge edge : input) {
            edgeList.add(edge.getEdge());
        }
        return edgeList;
    }

    @Override
    public List<ConnectedEdge> getInputEdges(BigDecimal minBw, int pri) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if ((pri < 0 || pri > 7) || (minBw == null)) {
            return liste;
        }
        for (ConnectedEdge edge : input) {
            /* First, check that edge attributes are present */
            if (edge.getEdge() == null || edge.getEdge().getEdgeAttributes() == null) {
                continue;
            }
            EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();
            if ((attributes.getUnreservedBandwidth() == null)
                    || (attributes.getMaxLinkBandwidth() == null)
                    || (attributes.getMaxResvLinkBandwidth() == null)) {
                continue;
            }
            List<BigDecimal> unRsvBw = attributes.getUnreservedBandwidth();

            /* Then, select only edge that meet constraints */
            if (unRsvBw.get(pri).longValue() >= minBw.longValue()
                && attributes.getMaxLinkBandwidth().longValue() >= minBw.longValue()
                && attributes.getMaxResvLinkBandwidth().longValue() >= minBw.longValue()) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public List<ConnectedEdge> getInputConnectedEdges() {
        return new ArrayList<ConnectedEdge>(this.input);
    }

    @Override
    public List<Edge> getOutputEdges() {
        ArrayList<Edge> edgeList = new ArrayList<Edge>();
        for (ConnectedEdge edge : output) {
            edgeList.add(edge.getEdge());
        }
        return edgeList;
    }

    @Override
    public List<ConnectedEdge> getOutputEdges(BigDecimal minBw, int pri) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if ((pri < 0 || pri > 7) || (minBw == null)) {
            return liste;
        }
        for (ConnectedEdge edge : output) {
            /* First, check that edge attributes are present */
            if (edge.getEdge() == null || edge.getEdge().getEdgeAttributes() == null) {
                continue;
            }
            EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();
            if ((attributes.getUnreservedBandwidth() == null)
                    || (attributes.getMaxLinkBandwidth() == null)
                    || (attributes.getMaxResvLinkBandwidth() == null)) {
                continue;
            }
            List<BigDecimal> unRsvBw = attributes.getUnreservedBandwidth();

            /* Then, select only edge that meet constraints */
            if (unRsvBw.get(pri).longValue() >= minBw.longValue()
                    && attributes.getMaxLinkBandwidth().longValue() >= minBw.longValue()
                    && attributes.getMaxResvLinkBandwidth().longValue() >= minBw.longValue()) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public List<ConnectedEdge> getOutputEdges(BigDecimal minBw, int pri, Delay delay, Uint32 te) {
        ArrayList<ConnectedEdge> liste = new ArrayList<ConnectedEdge>();
        if ((pri < 0 || pri > 7) || (minBw == null)) {
            return liste;
        }
        for (ConnectedEdge edge : output) {
            /* First, check that edge attributes are present */
            if (edge.getEdge() == null || edge.getEdge().getEdgeAttributes() == null) {
                continue;
            }
            EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();
            if ((attributes.getUnreservedBandwidth() == null)
                    || (attributes.getMaxLinkBandwidth() == null)
                    || (attributes.getMaxResvLinkBandwidth() == null)) {
                continue;
            }
            if (((te != null) && (attributes.getTeMetric() == null))
                    || ((delay != null) && (attributes.getDelay() == null))) {
                continue;
            }
            List<BigDecimal> unRsvBw = attributes.getUnreservedBandwidth();

            /* Then, select only edge that meet constraints */
            if (unRsvBw.get(pri).longValue() >= minBw.longValue()
                    && attributes.getMaxLinkBandwidth().longValue() >= minBw.longValue()
                    && attributes.getMaxResvLinkBandwidth().longValue() >= minBw.longValue()
                    && (te != null && attributes.getTeMetric().intValue() <= te.intValue())
                    && (delay != null && attributes.getDelay().getValue().intValue() <= delay.getValue().intValue())) {
                liste.add(edge);
            }
        }
        return liste;
    }

    @Override
    public List<ConnectedEdge> getOutputConnectedEdges() {
        return new ArrayList<ConnectedEdge>(this.output);
    }

    @Override
    public List<Prefix> getPrefixes() {
        return this.prefixes;
    }

    /**
     * Return the name of the associated Vertex if set or the router-id otherwise.
     *
     * @return Vertex name or router-id
    */
    @Override
    public String toString() {
        if (vertex.getName() != null) {
            return vertex.getName();
        } else {
            return vertex.getRouterId().toString();
        }
    }
}
