/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server.provider;

import com.google.common.base.Preconditions;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;

public class PceServerFactory implements PceServerProvider {

    private final ConnectedGraphProvider graphProvider;
    private final PathComputationProvider algoProvider;
    private ConnectedGraph tedGraph = null;

    public PceServerFactory(ConnectedGraphProvider graphProvider, PathComputationProvider pathComputationProvider) {
        Preconditions.checkArgument(graphProvider != null);
        this.graphProvider = graphProvider;
        this.algoProvider = pathComputationProvider;
        setTedGraph();
    }

    /**
     * Set Traffic Engineering Graph. This method is necessary as the TedGraph could be available
     * after the PathComputationFactory start e.g. manual insertion of a ted Graph, or late tedGraph fulfillment
     * from BGP Link State.
     */
    private void setTedGraph() {
        for (ConnectedGraph cgraph : this.graphProvider.getConnectedGraphs()) {
            if (cgraph.getGraph().getName().startsWith("ted://")) {
                this.tedGraph = cgraph;
                break;
            }
        }
    }

    @Override
    public PathComputationImpl getPathComputation() {
        /* Leave a change to get a valid Graph */
        if (tedGraph == null) {
            setTedGraph();
        }
        return new PathComputationImpl(tedGraph, algoProvider);
    }

    @Override
    public @Nullable ConnectedGraph getTedGraph() {
        /* Leave a change to get a valid Graph in case of late fulfillment */
        if (tedGraph == null) {
            setTedGraph();
        }
        return this.tedGraph;
    }
}
