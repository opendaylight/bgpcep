/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.algo;

import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AlgorithmType;

/**
 * This class provides access to Path Computation Algorithms.
 *
 * @author Olivier Dugeon
 *
 */
public interface PathComputationProvider {

    /**
     * Return Path Computation Algorithm object that corresponds to the requested type.
     *
     * @param cgraph         Connected Graph on which path computation will run
     * @param algorithmType  Algorithm supported types are: 'SPF', 'CSPF' and 'SAMCRA'
     *
     * @return PathComputationAlgorithm
     */
    PathComputationAlgorithm getPathComputationAlgorithm(ConnectedGraph cgraph, AlgorithmType algorithmType);

}
