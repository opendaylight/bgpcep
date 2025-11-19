/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.AlgorithmType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.GetConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.GetConstrainedPathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.GetConstrainedPathOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.GetConstrainedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.get.constrained.path.output.DivertPathBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path Computation Algorithms provider.
 *
 * @author Olivier Dugeon
 */
@Singleton
@Component(immediate = true, service = PathComputationProvider.class)
public final class PathComputationServer implements AutoCloseable, PathComputationProvider, GetConstrainedPath {
    private static final Logger LOG = LoggerFactory.getLogger(PathComputationServer.class);

    private final ConnectedGraphProvider graphProvider;
    private final Registration registration;

    @Inject
    @Activate
    public PathComputationServer(@Reference final RpcProviderService rpcService,
            @Reference final ConnectedGraphProvider graphProvider) {
        this.graphProvider = requireNonNull(graphProvider);
        registration = rpcService.registerRpcImplementation(this);
    }

    @Override
    public ListenableFuture<RpcResult<GetConstrainedPathOutput>> invoke(final GetConstrainedPathInput input) {
        final GetConstrainedPathOutputBuilder output = new GetConstrainedPathOutputBuilder();

        LOG.info("Got Path Computation Service request");

        /* First, get graph */
        final ConnectedGraph cgraph = graphProvider.getConnectedGraph(input.getGraphName());
        if (cgraph == null) {
            output.setStatus(ComputationStatus.Failed);
            return RpcResultBuilder.<GetConstrainedPathOutput>failed()
                    .withError(ErrorType.RPC, "Unknown Graph Name").buildFuture();
        }

        /* get a new Path Computation Algorithm according to Input choice */
        PathComputationAlgorithm algo = getPathComputationAlgorithm(cgraph, input.getAlgorithm());
        if (algo == null) {
            output.setStatus(ComputationStatus.Failed);
            return RpcResultBuilder.<GetConstrainedPathOutput>failed()
                    .withError(ErrorType.RPC, "Unknown Path Computation Algorithm").buildFuture();
        }

        /*
         * Request Path Computation for given source, destination and
         * constraints
         */
        final VertexKey source = new VertexKey(input.getSource());
        final VertexKey destination = new VertexKey(input.getDestination());
        LOG.info("Call Path Computation {} algorithm for path from {} to {} with contraints {}",
                input.getAlgorithm().getName(), source, destination, input.getConstraints());
        ConstrainedPath cpath;
        if (input.getConstraints().getPathDiversity() == null) {
            cpath = algo.computeP2pPath(source, destination, input.getConstraints());
        } else {
            cpath = algo.computeDivertPaths(source, destination, input.getConstraints());
        }
        LOG.info("Path Computation {} algorithm returned path: {}", input.getAlgorithm().getName(), cpath);

        output.setStatus(cpath.getStatus())
            .setSource(cpath.getSource())
            .setDestination(cpath.getDestination())
            .setPathDescription(cpath.getPathDescription())
            .setComputedMetric(cpath.getComputedMetric())
            .setComputedTeMetric(cpath.getComputedTeMetric())
            .setComputedDelay(cpath.getComputedDelay());
        if (input.getConstraints().getPathDiversity() != null && cpath.getDivertPath() != null) {
            output.setDivertPath(new DivertPathBuilder(cpath.getDivertPath()).build());
        }

        /* Send back the Computed Path */
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        registration.close();
    }

    @Override
    public PathComputationAlgorithm getPathComputationAlgorithm(final ConnectedGraph runningGraph,
            final AlgorithmType algorithmType) {
        PathComputationAlgorithm algo = null;
        switch (algorithmType) {
            case Spf:
                algo = new ShortestPathFirst(runningGraph);
                break;
            case Cspf:
                algo = new ConstrainedShortestPathFirst(runningGraph);
                break;
            case Samcra:
                algo = new Samcra(runningGraph);
                break;
            default:
                break;
        }
        return algo;
    }
}
