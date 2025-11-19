/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.algo;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.constraints.Constraints;

/**
 * This class provides entry for various Path Computation Algorithms.
 *
 * @author Olivier Dugeon
 *
 */
public interface PathComputationAlgorithm {

    /**
     * Compute Point to Point Path from source to destination taking into account constraints.
     *
     * @param source      Source Vertex Key
     * @param destination Destination Vertex Key
     * @param constraints Constraints (Metric, TE Metric, Delay, Jitter, Loss, Bandwidth)
     *
     * @return A Path that meet constraints or empty path otherwise. ConstrainedPath.Status indicates the result of
     *         the path computation (Completed or Failed)
     */
    @NonNull ConstrainedPath computeP2pPath(VertexKey source, VertexKey destination, Constraints constraints);

    /**
     * Compute point to point divert path from source to destination taking into account specified constraints.
     * The secondary path could use the same source / destination endpoints or a new endpoint pair could be specified
     * within the diversity Object under the constraints.
     *
     * @param source      Source Vertex Key
     * @param destination Destination Vertex Key
     * @param constraints Constraints including diversity (link, node, srlg and endpoints if different)
     *
     * @return Two diverted paths that meet constraints or empty paths otherwise. ConstrainedPath.Status indicates the
     *         result of the path computation (Completed or Failed). SecondaryPath contains the path description of the
     *         diverted path.
     */
    @NonNull ConstrainedPath computeDivertPaths(VertexKey source, VertexKey destination, Constraints constraints);
}
