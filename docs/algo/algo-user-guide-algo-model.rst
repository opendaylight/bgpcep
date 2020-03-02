.. _algo-user-guide-algo-model:

Path Computation Algorithms Overview
====================================

This section provides a high-level overview about Path Computation Algorithms.

.. contents:: Contents
   :depth: 2
   :local:

Path Computation Theory
^^^^^^^^^^^^^^^^^^^^^^^

Path computation in network has for objective to find a path between two end
points (p2p) or between a point and a multiple destination points (p2mp). The
well known algorithm is the Djikstra one whch aim to find the shortest path
by taking into account the number of hop as key objective.

In addition, path computation aims also to take into account various constraints
to find optimal path in a network. The constraints are various and may include
standard routing protcol metric (IGP metric), Traffic Enginnering metic (TE
metric), end to end delay (Delay), end to end delay variation (Jitter) ...
all referenced as *Additive Metric* because the metric carried by each link is
added together to check that the end-to-end constraint is respected. The second
category of metric is named *Concave Metric* and concerns the Bandwidth and
Loss. Indeed, these metrics are not added together but checked over each link
to verify that the constraints are met.

For more information, reader could refer to Path Computation algorithms
e.g. Shortest Path First (SPF) https://en.wikipedia.org/wiki/Shortest_path_problem
and Constrainted Shortest Path First (CSPF) https://en.wikipedia.org/wiki/Constrained_Shortest_Path_First.

Path Computation Overview
^^^^^^^^^^^^^^^^^^^^^^^^^

This features provides three Path Computation algorithms:

 * Shortest Path First (SPF) a.k.a. Djikstra
 * Constrainted Shortest Path First (CSPF)
 * Self Adaptive Multiple Constraints Routing Algorithm (SAMCRA)

All of these algorithms use the same principles:

 * A priority Queue where all potential paths are stored
 * A pruning function that validate / invalidate edge to next vertex regarding
   the constraints

The priority queue sort elements based on a key and outpout the element that
presents the smallest key value. Here, depedning of the algorithm, the key will
represents the standard metric (SPF), the Traffic Engineering Metric (CSPF) or
the TE Metric and Delay as composite metric. The key represents only *Additive
Metrics* to be optimized.

The pruning function will check if edge to the next vertex respects the given
constraints. This concerns both *Concave Metrics*, bandwidth and loss, and
*Additive Metrics*. For the latter, current metrics value are added to the
edge metrics value and check against given constraints. In addition, address
family (IPv4, IPv6, Segment Routing for IPv4 or IPv6) is checked to avoid
validate a path that is not capable of the given requestes address family
(e.g. an IPv4 vertex / edge for an IPv6 path).

The pseudo algorithm below shows how the various algorithms are working:

.. code-block:: java

    /* Initialize the algorithms */
    initialize pathSource and pathDestination with vertex source and Destination;
    visitedVertexList.clear();
    processedPathList.clear();
    priorityQueue.clear();
    priorityQueue.add(pathSource);
    currentMetric = Integer.MAX_VALUE;
    computedPath = null;

    /* Loop until Priority Queue becomes empty */
    while (!priorityQueue.empty()) {
        /* Get currentPath with lowest accumulated metric from the Priority Queue */
        currentPath = priorityQueue.poll();

        /* For all Edges from the current vertex, check if next Vertex is acceptable or not */
        for (edge : currentPath.getvertex().getAllEdges()) {
            if (pruneEdge(edge, currentPath)) {
                continue;
            }
            /* If we reach destination with a better Metric, store the path */
            if (relax(edge, currentPath) && (pathDestination.getMetric() < currentMetric))
                computedPath = pathDestination;
            }
        }
    }

    /* Example of relax function that checks the standard routing Metric */
    private boolean relax(edge, currentPath) {
        /* Verify if we have not visited this Vertex to avoid loop */
        if (visitedVerticeList.contains(edge.getDestination())) {
            return false;
        }

        /* Get Next Vertex from processedPathList or create a new one if it has not yet processed */
        nextPath = processedPathList.get(edge.getDestination());
        if (nextPath == null) {
            nextPath = new (edge.getDestination());
            processedPathList.add(nextPath);
        }

        /* Compute Metric from source to this next Vertex and add or update it in the Priority Queue
         * if total path Metric is lower than metric associated to this next Vertex.
         * This could occurs if we process a Vertex that as not yet been visited in the Graph
         * or if we found a shortest path up to this Vertex. */
        int totalMetric = edge.getMetric() + currentPath.getMetric();
        if (nextPath.getMetric() > totalMetric) {
            nextPath = currentPath;
            nextPath.setMetric(totalMetric);
            nextPath.addEdgeToPath(edge);
            /* Here, we set the path key with the total Metric for the  Priority Queue
             * At next iteration, Priority Queue will consider this new Path in the collection
             * to provide the path with the lowest total Metric */
            nextPath.setKey(totalMetric);
            priorityQueue.add(nextPath);
        }
        /* Return True if we reach the destination, false otherwise */
        return pathDestination.equals(nextPath);
    } 

    /* Example of prune function that checks bandwidth and standard metric */
    boolean pruneEdge(edge, currentPath) {
        if (edge.getBandwidth() < constraints.getBandwidth()) {
            return true;
        }
        if (edge.getMetric() + currentPath.getMetric() > constraints.getMetric()) {
            return true;
        }
    }

This pseudo code corresponds to the ShortestPathFist.java class.

Note: Details of SAMCRA algorithm could be found in the article "Concepts of
Exact QoS Routing Algorithms", Piet Van Mieghem and Fernando A. Kuipers,
IEEE/ACM Transactions on Networking, Volume 12, Number 5, October 2004.

