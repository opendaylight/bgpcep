/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.GetConstrainedPathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.GetConstrainedPathOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.GetConstrainedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathComputationService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Path Computation Algorithms provider.
 *
 * @author Olivier Dugeon
 */
public class PathComputationProvider implements AutoCloseable, PathComputationService {

    private ObjectRegistration<PathComputationService> pathService;
    private RpcProviderService rpcProviderRegistry;
    private ConnectedGraphProvider graphProvider;

    private ConnectedGraph cgraph;

    public PathComputationProvider(final RpcProviderService rpcService, final ConnectedGraphProvider graphProvider) {
        checkArgument(rpcService != null);
        checkArgument(graphProvider != null);
        this.rpcProviderRegistry = rpcService;
        this.graphProvider = graphProvider;
    }

    public void init() {
        pathService = this.rpcProviderRegistry.registerRpcImplementation(PathComputationService.class, this);
    }

    @Override
    public ListenableFuture<RpcResult<GetConstrainedPathOutput>> getConstrainedPath(
            final GetConstrainedPathInput input) {
        final GetConstrainedPathOutputBuilder output = new GetConstrainedPathOutputBuilder();

        /* First, get graph */
        cgraph = graphProvider.getConnectedGraph(input.getGraphName());
        if (cgraph == null) {
            output.setStatus(ComputationStatus.Failed);
            return RpcResultBuilder.<GetConstrainedPathOutput>failed()
                    .withError(RpcError.ErrorType.RPC, "Unknown Graph Name")
                    .buildFuture();
        }

        /* Determine Path Computation Algorithm according to Input choice */
        PathComputationAlgorithm algo = null;
        switch (input.getAlgorithm().getIntValue()) {
            case 0:
                algo = new ShortestPathFirst(cgraph);
                break;
            case 1:
                algo = new ConstrainedShortestPathFirst(cgraph);
                break;
            case 2:
                algo = new Samcra(cgraph);
                break;
            default:
                break;
        }

        if (algo == null) {
            output.setStatus(ComputationStatus.Failed);
            return RpcResultBuilder.<GetConstrainedPathOutput>failed()
                    .withError(RpcError.ErrorType.RPC, "Unknown Path Computation Algorithm")
                    .buildFuture();
        }

        /* Request Path Computation for given source, destination and constraints */
        final VertexKey source = new VertexKey(input.getSource());
        final VertexKey destination = new VertexKey(input.getSource());
        final ConstrainedPath cpath = algo.computeP2pPath(source, destination, input.getConstraints());

        /* Send back the Computed Path */
        output.setPathDescription(cpath.getPathDescription())
                .setStatus(cpath.getStatus())
                .setComputedMetric(cpath.getMetric())
                .setComputedTeMetric(cpath.getTeMetric())
                .setComputedDelay(cpath.getDelay());

        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public void close() throws Exception {
        pathService.close();
    }
}
