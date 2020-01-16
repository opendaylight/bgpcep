/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.algo;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.Constraints;

/**
 * This class provide entry for various Path Computation Algorithms.
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
     * @param constraints Constraints (Metric, TE Metric, Delay, Jitter, Bandwidth)
     *
     * @return A Path that meet constraints or empty path otherwise (see Status)
     */
    @NonNull ConstrainedPath computeP2pPath(VertexKey source, VertexKey destination, Constraints constraints);
}
