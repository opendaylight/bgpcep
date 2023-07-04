/*
 * Copyright (c) 2019 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedEdgeTrigger;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphTrigger;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.graph.ConnectedVertexTrigger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements the Connected Graph for path computation algorithms.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
public class ConnectedGraphImpl implements ConnectedGraph {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedGraphImpl.class);

    /* List of Connected Vertices that composed this Connected Graph */
    private final HashMap<Long, ConnectedVertexImpl> vertices = new HashMap<>();

    /* List of Connected Edges that composed this Connected Graph */
    private final HashMap<Long, ConnectedEdgeImpl> edges = new HashMap<>();

    /* List of IP prefix attached to Vertices */
    private final HashMap<IpPrefix, Prefix> prefixes = new HashMap<>();

    /* List of Triggers attached to the Connected Graph */
    private final ConcurrentHashMap<TopologyKey, ConnectedGraphTrigger> graphTriggers = new ConcurrentHashMap<>();
    // FIXME: this service is never shut down
    private final ExecutorService exec = Executors.newCachedThreadPool();

    /* Reference to the non connected Graph stored in DataStore */
    private Graph graph;

    /* Reference to Graph Model Server to store corresponding graph in DataStore */
    private final ConnectedGraphServer connectedGraphServer;

    public ConnectedGraphImpl(final Graph newGraph, final ConnectedGraphServer server) {
        graph = newGraph;
        createConnectedGraph();
        connectedGraphServer = server;
    }

    /**
     * Transform the associated Graph in a Connected Graph. This method will automatically create associated Connected
     * Vertices, from the Graph Vertices, Connected Edges, from the Graph Edges and Prefix from the Graph Prefix.
     *
     */
    private void createConnectedGraph() {
        if (graph == null) {
            return;
        }
        /* Add all vertices */
        for (Vertex vertex : graph.nonnullVertex().values()) {
            ConnectedVertexImpl cvertex = new ConnectedVertexImpl(vertex);
            vertices.put(cvertex.getKey(), cvertex);
        }
        /* Add all edges */
        for (Edge edge : graph.nonnullEdge().values()) {
            ConnectedEdgeImpl cedge = new ConnectedEdgeImpl(edge);
            edges.put(cedge.getKey(), cedge);
        }
        /* Add all prefixes */
        for (Prefix prefix : graph.nonnullPrefix().values()) {
            ConnectedVertexImpl cvertex = vertices.get(prefix.getVertexId().longValue());
            if (cvertex != null) {
                cvertex.addPrefix(prefix);
            }
            prefixes.putIfAbsent(prefix.getPrefix(), prefix);
        }
    }

    /**
     * Return Connected Vertex if it exists or create a new one.
     *
     * @param  key   Unique Vertex Key identifier
     * @return new or existing Connected Vertex
     */
    private ConnectedVertexImpl updateConnectedVertex(final @NonNull Long key) {
        checkArgument(key != 0, "Provided Vertex Key must not be equal to 0");
        ConnectedVertexImpl vertex = vertices.get(key);
        if (vertex == null) {
            vertex = new ConnectedVertexImpl(key);
            vertices.put(key, vertex);
        }
        return vertex;
    }

    /**
     * Return Connected Edge if it exist or create a new one.
     *
     * @param key   Unique Edge Key identifier
     * @return new or existing Connected Edge
     */
    private ConnectedEdgeImpl updateConnectedEdge(final @NonNull Long key) {
        checkArgument(key != 0, "Provided Edge Key must not be equal to 0");
        ConnectedEdgeImpl edge = edges.get(key);
        if (edge == null) {
            edge = new ConnectedEdgeImpl(key);
            edges.put(edge.getKey(), edge);
        }
        return edge;
    }

    /**
     * Connect source and destination Connected Vertices with the given Connected Edge.
     *
     * @param srcVertex Source Connected Vertex
     * @param dstVertex Destination Connected Vertex
     * @param edge      Connected Edge
     */
    private static void connectVertices(final ConnectedVertexImpl srcVertex, final ConnectedVertexImpl dstVertex,
            final ConnectedEdgeImpl edge) {
        if (edge != null) {
            edge.setSource(srcVertex);
            edge.setDestination(dstVertex);
        }
        if (srcVertex != null) {
            srcVertex.addOutput(edge);
        }
        if (dstVertex != null) {
            dstVertex.addInput(edge);
        }
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public List<ConnectedVertex> getVertices() {
        return new ArrayList<>(vertices.values());
    }

    @Override
    public ConnectedVertex getConnectedVertex(final Long key) {
        return vertices.get(key);
    }

    @Override
    public ConnectedVertex getConnectedVertex(final IpAddress address) {
        if (address == null) {
            return null;
        }
        IpPrefix prefix = null;
        if (address.getIpv4Address() != null) {
            prefix = new IpPrefix(new Ipv4Prefix(address.getIpv4Address().getValue() + "/32"));
        }
        if (address.getIpv6Address() != null) {
            prefix = new IpPrefix(new Ipv6Prefix(address.getIpv6Address().getValue() + "/128"));
        }
        if (prefix != null && prefixes.containsKey(prefix)) {
            long key = prefixes.get(prefix).getVertexId().longValue();
            return vertices.get(key);
        } else {
            return null;
        }
    }

    @Override
    public int getVerticesSize() {
        return vertices.size();
    }

    @Override
    public List<ConnectedEdge> getEdges() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public ConnectedEdge getConnectedEdge(final Long key) {
        return edges.get(key);
    }

    @Override
    public ConnectedEdge getConnectedEdge(final IpAddress address) {
        if (address == null) {
            return null;
        }
        if (address.getIpv4Address() != null) {
            return getConnectedEdge(address.getIpv4Address());
        }
        if (address.getIpv6Address() != null) {
            return getConnectedEdge(address.getIpv6Address());
        }
        return null;
    }

    @Override
    public ConnectedEdge getConnectedEdge(final Ipv4Address address) {
        return address == null ? null : getConnectedEdge(
            Uint32.fromIntBits(IetfInetUtil.ipv4AddressBits(address)).longValue());
    }

    @Override
    public ConnectedEdge getConnectedEdge(final Ipv6Address address) {
        return address == null ? null : getConnectedEdge(
            ByteBuffer.wrap(IetfInetUtil.ipv6AddressBytes(address), Long.BYTES, Long.BYTES).getLong());
    }

    @Override
    public int getEdgesSize() {
        return edges.size();
    }

    @Override
    public List<Prefix> getPrefixes() {
        return new ArrayList<>(prefixes.values());
    }

    @Override
    public Prefix getPrefix(final IpPrefix prefix) {
        return prefixes.get(prefix);
    }

    private void callVertexTrigger(final ConnectedVertexImpl cvertex, final Vertex vertex) {
        List<ConnectedVertexTrigger> vertexTriggers = cvertex.getTriggers();
        if (vertexTriggers == null || vertexTriggers.isEmpty()) {
            return;
        }
        for (ConnectedGraphTrigger trigger : graphTriggers.values()) {
            exec.execute(() -> trigger.verifyVertex(vertexTriggers, cvertex, vertex));
        }
    }

    @Override
    public ConnectedVertex addVertex(final Vertex vertex) {
        checkArgument(vertex != null, "Provided Vertex is a null object");
        ConnectedVertexImpl cvertex = updateConnectedVertex(vertex.getVertexId().longValue());
        Vertex old = cvertex.getVertex();
        connectedGraphServer.addVertex(graph, vertex, old);
        cvertex.setVertex(vertex);
        if (old != null) {
            callVertexTrigger(cvertex, old);
        }
        return cvertex;
    }

    @Override
    public void deleteVertex(final VertexKey key) {
        checkArgument(key != null, "Provided Vertex Key is a null object");
        ConnectedVertexImpl cvertex = vertices.get(key.getVertexId().longValue());
        if (cvertex != null) {
            cvertex.disconnect();
            vertices.remove(cvertex.getKey());
            connectedGraphServer.deleteVertex(graph, cvertex.getVertex());
            cvertex.setVertex(null);
            callVertexTrigger(cvertex, null);
        }
    }

    private void callEdgeTrigger(final ConnectedEdgeImpl cedge, final Edge edge) {
        List<ConnectedEdgeTrigger> edgeTriggers = cedge.getTriggers();
        if (edgeTriggers == null || edgeTriggers.isEmpty()) {
            return;
        }
        for (ConnectedGraphTrigger trigger : graphTriggers.values()) {
            exec.execute(() -> trigger.verifyEdge(edgeTriggers, cedge, edge));
        }
    }

    @Override
    public ConnectedEdge addEdge(final Edge edge) {
        checkArgument(edge != null, "Provided Edge is a null object");
        ConnectedEdgeImpl cedge = updateConnectedEdge(edge.getEdgeId().longValue());
        Edge old = cedge.getEdge();
        if (old == null) {
            ConnectedVertexImpl source = null;
            ConnectedVertexImpl destination = null;
            if (edge.getLocalVertexId() != null) {
                source = updateConnectedVertex(edge.getLocalVertexId().longValue());
            }
            if (edge.getRemoteVertexId() != null) {
                destination = updateConnectedVertex(edge.getRemoteVertexId().longValue());
            }
            connectVertices(source, destination, cedge);
        }
        connectedGraphServer.addEdge(graph, edge, old);
        cedge.setEdge(edge);
        callEdgeTrigger(cedge, old);
        return cedge;
    }

    /**
     * Connected Edge is kept in the edges Hash Map in order to memorize the total Bandwidth reserved by
     * Constrained Paths that belong to this Edge. Connected Edges are removed when the Connected Graph is cleared.
     */
    @Override
    public void deleteEdge(final EdgeKey key) {
        checkArgument(key != null, "Provided Edge Key is a null object");
        ConnectedEdgeImpl cedge = edges.get(key.getEdgeId().longValue());
        if (cedge != null) {
            connectedGraphServer.deleteEdge(graph, cedge.getEdge());
            cedge.disconnect();
            cedge.setEdge(null);
            callEdgeTrigger(cedge, null);
        }
    }

    @Override
    public void addPrefix(final Prefix prefix) {
        checkArgument(prefix != null, "Provided Prefix is a null object");
        ConnectedVertexImpl cvertex = updateConnectedVertex(prefix.getVertexId().longValue());
        cvertex.addPrefix(prefix);
        prefixes.putIfAbsent(prefix.getPrefix(), prefix);
        connectedGraphServer.addPrefix(graph, prefix);
    }

    @Override
    public void deletePrefix(final IpPrefix ippfx) {
        checkArgument(ippfx != null, "Provided Prefix is a null object");
        Prefix prefix = prefixes.get(ippfx);
        if (prefix != null) {
            ConnectedVertexImpl cvertex = vertices.get(prefix.getVertexId().longValue());
            if (cvertex != null) {
                cvertex.removePrefix(prefix);
            }
            prefixes.remove(prefix.getPrefix());
            connectedGraphServer.deletePrefix(graph, prefix);
        }
    }

    @Override
    public void clear() {
        LOG.info("Reset Connected Graph({})", graph.getName());
        vertices.clear();
        edges.clear();
        prefixes.clear();
        connectedGraphServer.clearGraph(graph);
        graph = null;
    }

    @Override
    public String getSummary() {
        return vertices.size() + "/" + edges.size() + "/" + prefixes.size();
    }

    @Override
    public boolean registerTrigger(final ConnectedGraphTrigger trigger, final TopologyKey key) {
        return graphTriggers.putIfAbsent(key, trigger) == null;
    }

    @Override
    public boolean unRegisterTrigger(final ConnectedGraphTrigger trigger, final TopologyKey key) {
        return graphTriggers.remove(key, trigger);
    }

    /**
     * Returns the name of the associated Graph.
     *
     * @return Graph name
     */
    @Override
    public String toString() {
        return graph.getName();
    }
}
