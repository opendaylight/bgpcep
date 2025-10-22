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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.graph.ConnectedVertexTrigger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Vertex;

/**
 * This Class implements the Connected Vertex used by the Connected Graph for path computation algorithms.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
public class ConnectedVertexImpl implements ConnectedVertex {

    /* Reference to input and output Connected Edge within the Connected Graph */
    private final ArrayList<ConnectedEdgeImpl> input = new ArrayList<>();
    private final ArrayList<ConnectedEdgeImpl> output = new ArrayList<>();

    /* List of Prefixes announced by this Vertex */
    private final ArrayList<Prefix> prefixes = new ArrayList<>();

    /* Reference to the Vertex of the standard Graph associated to the Connected Graph */
    private Vertex vertex = null;

    /* Connected Vertex Identifier */
    private final @NonNull Long cvid;

    /* Path Diversity: True if Vertex is used by the primary path, false otherwise */
    @GuardedBy("this")
    private final AtomicBoolean diversity = new AtomicBoolean(false);

    /* List of Connected Edge Trigger */
    private final ConcurrentMap<String, ConnectedVertexTrigger> triggers = new ConcurrentHashMap<>();

    public ConnectedVertexImpl(final @NonNull Long key) {
        checkArgument(key != 0, "Vertex Key must not be equal to 0");
        cvid = key;
        vertex = null;
    }

    public ConnectedVertexImpl(final @NonNull Vertex vertex) {
        final var id = vertex.getVertexId().longValue();
        checkArgument(id != 0, "Vertex Key must not be equal to 0");
        cvid = id;
        this.vertex = vertex;
    }

    /**
     * When vertex is removed, we must disconnect all Connected Edges.
     */
    void close() {
        triggers.clear();
        disconnect();
    }

    /**
     * Set associated Vertex to this Connected Vertex.
     *
     * @param vertex Vertex
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl setVertex(final Vertex vertex) {
        this.vertex = vertex;
        return this;
    }

    /**
     * Add Connected Edge as input edge.
     *
     * @param edge Connected Edge
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl addInput(final ConnectedEdgeImpl edge) {
        if (!input.contains(edge)) {
            input.add(edge);
        }
        return this;
    }

    /**
     * Add Connected Edge as output edge.
     *
     * @param edge Connected Edge
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl addOutput(final ConnectedEdgeImpl edge) {
        if (!output.contains(edge)) {
            output.add(edge);
        }
        return this;
    }

    /**
     * Remove input Connected Edge.
     *
     * @param edge Connected Edge
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl removeInput(final ConnectedEdgeImpl edge) {
        input.remove(edge);
        return this;
    }

    /**
     * Remove output Connected Edge.
     *
     * @param edge Connected Edge
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl removeOutput(final ConnectedEdgeImpl edge) {
        output.remove(edge);
        return this;
    }

    /**
     * Disconnect all input and output Connected Edge.
     */
    public void disconnect() {
        for (var edge : input) {
            edge.disconnectDestination();
        }
        for (var edge : output) {
            edge.disconnectSource();
        }
    }

    /**
     * Add Prefix to this Connected Vertex.
     *
     * @param prefix Prefix
     * @return This Connected Vertex
     */
    public ConnectedVertexImpl addPrefix(final Prefix prefix) {
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
    public void removePrefix(final Prefix prefix) {
        if (prefixes.contains(prefix)) {
            prefixes.remove(prefix);
        }
    }

    @Override
    public Long getKey() {
        return cvid;
    }

    @Override
    public Vertex getVertex() {
        return vertex;
    }

    @Override
    public List<ConnectedEdge> getEdgeTo(final Long dstRid) {
        return output.stream()
            .filter(edge -> edge.getDestination().getKey().equals(dstRid))
            .collect(Collectors.toList());
    }

    @Override
    public List<Edge> getInputEdges() {
        return input.stream().map(ConnectedEdgeImpl::getEdge).collect(Collectors.toList());
    }

    @Override
    public List<ConnectedEdge> getInputConnectedEdges() {
        return new ArrayList<>(input);
    }

    @Override
    public List<Edge> getOutputEdges() {
        return output.stream().map(ConnectedEdgeImpl::getEdge).collect(Collectors.toList());
    }

    @Override
    public List<ConnectedEdge> getOutputConnectedEdges() {
        return new ArrayList<>(output);
    }

    @Override
    public List<Prefix> getPrefixes() {
        return prefixes;
    }

    @Override
    public boolean registerTrigger(final ConnectedVertexTrigger trigger, final String key) {
        return triggers.putIfAbsent(key, trigger) == null;
    }

    @Override
    public boolean unRegisterTrigger(final ConnectedVertexTrigger trigger, final String key) {
        return triggers.remove(key, trigger);
    }

    public List<ConnectedVertexTrigger> getTriggers() {
        return new ArrayList<>(triggers.values());
    }

    @Override
    public boolean isDivert() {
        return diversity.get();
    }

    @Override
    public void setDiversity(boolean used) {
        diversity.set(used);
    }

    /**
     * Return the name of the associated Vertex if set or the router-id otherwise.
     *
     * @return Vertex name or router-id
    */
    @Override
    public String toString() {
        if (vertex == null) {
            return "Null";
        }
        if (vertex.getName() != null) {
            return vertex.getName();
        } else {
            return vertex.getRouterId().toString();
        }
    }
}
