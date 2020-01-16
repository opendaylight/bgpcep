/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.path.computation.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.Constraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.path.description.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.path.description.EdgeBuilder;

public abstract class AbstractPathComputation implements PathComputationAlgorithm {

    protected final ConnectedGraph graph;

    protected CspfPath pathSource = null;
    protected CspfPath pathDestination = null;

    protected PriorityQueue<CspfPath> priorityQueue;
    protected HashMap<Long, CspfPath> processedPath;

    protected AbstractPathComputation(ConnectedGraph graph) {
        this.graph = graph;
        priorityQueue = new PriorityQueue<CspfPath>(graph.getVerticesSize());
        processedPath = new HashMap<Long, CspfPath>(graph.getVerticesSize());
    }

    protected ConstrainedPathBuilder initializePathComputation(VertexKey src, VertexKey dst) {
        ConstrainedPathBuilder cpathBuilder = new ConstrainedPathBuilder().setStatus(ComputationStatus.OnGoing);

        /* Check that source and destination vertexKey are not identical */
        if (src.equals(dst)) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }

        /* Get the Connected Vertex from the Graph to initialize the source of the Samcra Path */
        ConnectedVertex vertex = null;
        vertex = graph.getConnectedVertex(src.getVertexId().longValue());
        if (vertex == null) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }
        pathSource = new CspfPath(vertex).setCost(0).setDelay(0);
        cpathBuilder.setSource(vertex.getVertex().getVertexId());

        /* Get the Connected Vertex from the Graph to initialize the destination of the Samcra Path */
        vertex = graph.getConnectedVertex(dst.getVertexId().longValue());
        if (vertex == null) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }
        pathDestination = new CspfPath(vertex).setCost(-1).setDelay(-1);
        cpathBuilder.setDestination(vertex.getVertex().getVertexId());

        /* Mark the Constrained Path as On Going */
        cpathBuilder.setStatus(ComputationStatus.OnGoing);

        /* Initialize the Priority Queue, HashMap */
        priorityQueue.clear();
        priorityQueue.add(pathSource);
        processedPath.clear();
        processedPath.put(pathSource.getVertexKey(), pathSource);
        processedPath.put(pathDestination.getVertexKey(), pathDestination);

        return cpathBuilder;
    }

    protected List<Edge> getPathList(List<ConnectedEdge> edges) {
        ArrayList<Edge> list = new ArrayList<Edge>();

        for (ConnectedEdge edge : edges) {
            list.add(new EdgeBuilder(edge.getEdge()).build());
        }
        return list;
    }

    @Override
    public abstract ConstrainedPath computeP2pPath(VertexKey source, VertexKey destination, Constraints constraints);

}
