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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
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

    /* List of Connected Vertics that composed this Connected Graph */
    private final HashMap<Long, ConnectedVertexImpl> vertices = new HashMap<>();

    /* List of Connected Edges that composed this Connected Graph */
    private final HashMap<Long, ConnectedEdgeImpl> edges = new HashMap<>();

    /* List of IP prefix attached to Vertices */
    private final HashMap<IpPrefix, Prefix> prefixes = new HashMap<>();

    /* Reference to the non connected Graph stored in DataStore */
    private Graph graph;

    /* Reference to Graph Model Server to store corresponding graph in DataStore */
    private final ConnectedGraphServer connectedGraphServer;

    public ConnectedGraphImpl(final Graph newGraph, final ConnectedGraphServer server) {
        this.graph = newGraph;
        createConnectedGraph();
        this.connectedGraphServer = server;
    }

    /**
     * Transform the associated Graph in a Connected Graph. This method will automatically create associated Connected
     * Vertices, from the Graph Vertices, Connected Edges, from the Graph Edges and Prefix from the Graph Prefix.
     *
     */
    private void createConnectedGraph() {
        if (this.graph == null) {
            return;
        }
        /* Add all vertices */
        for (Vertex vertex : this.graph.nonnullVertex().values()) {
            ConnectedVertexImpl cvertex = new ConnectedVertexImpl(vertex);
            vertices.put(cvertex.getKey(), cvertex);
        }
        /* Add all edges */
        for (Edge edge : this.graph.nonnullEdge().values()) {
            ConnectedEdgeImpl cedge = new ConnectedEdgeImpl(edge);
            edges.put(cedge.getKey(), cedge);
        }
        /* Add all prefixes */
        for (Prefix prefix : this.graph.nonnullPrefix().values()) {
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
        return this.graph;
    }

    @Override
    public List<ConnectedVertex> getVertices() {
        return new ArrayList<>(this.vertices.values());
    }

    @Override
    public ConnectedVertex getConnectedVertex(final Long key) {
        return vertices.get(key);
    }

    @Override
    public ConnectedVertex getConnectedVertex(final IpAddress address) {
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
        return new ArrayList<>(this.edges.values());
    }

    @Override
    public ConnectedEdge getConnectedEdge(final Long key) {
        return edges.get(key);
    }

    @Override
    public ConnectedEdge getConnectedEdge(final IpAddress address) {
        Uint64 key;
        if (address.getIpv4Address() != null) {
            key = Uint32.fromIntBits(IetfInetUtil.INSTANCE.ipv4AddressBits(address.getIpv4Address())).toUint64();
            return getConnectedEdge(key.longValue());
        }
        if (address.getIpv6Address() != null) {
            final byte[] ip = IetfInetUtil.INSTANCE.ipv6AddressBytes(address.getIpv6Address());
            key = Uint64.fromLongBits(ByteBuffer.wrap(ip, Long.BYTES, Long.BYTES).getLong());
            return getConnectedEdge(key.longValue());
        }
        return null;
    }

    @Override
    public int getEdgesSize() {
        return edges.size();
    }

    @Override
    public List<Prefix> getPrefixes() {
        return new ArrayList<>(this.prefixes.values());
    }

    @Override
    public Prefix getPrefix(final IpPrefix prefix) {
        return this.prefixes.get(prefix);
    }

    @Override
    public ConnectedVertex addVertex(final Vertex vertex) {
        checkArgument(vertex != null, "Provided Vertex is a null object");
        ConnectedVertexImpl cvertex = updateConnectedVertex(vertex.getVertexId().longValue());
        Vertex old = cvertex.getVertex();
        this.connectedGraphServer.addVertex(this.graph, vertex, old);
        cvertex.setVertex(vertex);
        return cvertex;
    }

    @Override
    public void deleteVertex(final VertexKey key) {
        checkArgument(key != null, "Provided Vertex Key is a null object");
        ConnectedVertexImpl cvertex = vertices.get(key.getVertexId().longValue());
        if (cvertex != null) {
            cvertex.disconnect();
            vertices.remove(cvertex.getKey());
            this.connectedGraphServer.deleteVertex(this.graph, cvertex.getVertex());
            cvertex.setVertex(null);
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
        this.connectedGraphServer.addEdge(this.graph, edge, old);
        cedge.setEdge(edge);
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
            this.connectedGraphServer.deleteEdge(this.graph, cedge.getEdge());
            cedge.disconnect();
            cedge.setEdge(null);
        }
    }

    @Override
    public void addPrefix(final Prefix prefix) {
        checkArgument(prefix != null, "Provided Prefix is a null object");
        ConnectedVertexImpl cvertex = updateConnectedVertex(prefix.getVertexId().longValue());
        cvertex.addPrefix(prefix);
        prefixes.putIfAbsent(prefix.getPrefix(), prefix);
        this.connectedGraphServer.addPrefix(this.graph, prefix);
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
            this.connectedGraphServer.deletePrefix(this.graph, prefix);
        }
    }

    @Override
    public void clear() {
        LOG.info("Reset Connected Graph({})", graph.getName());
        this.vertices.clear();
        this.edges.clear();
        this.prefixes.clear();
        this.connectedGraphServer.clearGraph(this.graph);
        this.graph = null;
    }

    @Override
    public String getSummary() {
        return vertices.size() + "/" + edges.size() + "/" + prefixes.size();
    }

    /**
     * Returns the name of the associated Graph.
     *
     * @return Graph name
     */
    @Override
    public String toString() {
        return this.graph.getName();
    }
}
