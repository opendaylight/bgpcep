/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server.provider;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.AlgorithmType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.get.constrained.path.input.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationImpl implements PathComputation {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationImpl.class);

    private final ConnectedGraph tedGraph;
    private final PathComputationProvider algoProvider;

    public PathComputationImpl(ConnectedGraph tedGraph, PathComputationProvider algoProvider) {
        Preconditions.checkArgument(tedGraph != null);
        this.tedGraph = tedGraph;
        this.algoProvider = algoProvider;
    }

    @Override
    public Message computePath(Requests req) {
        LOG.info("Received Compute Path request");

        /* Check that Request Parameter Object is present */
        if (req == null || req.getRp() == null) {
            LOG.error("Missing Request Parameter Objects. Abort!");
            return MessagesUtil.createErrorMsg(PCEPErrors.RP_MISSING, Uint32.ZERO);
        }

        LOG.debug("Request for path computation {}", req);

        /*
         * Check that mandatory End Point Objects are present and Source /
         * Destination are know in the TED Graph
         */
        P2p input = req.getSegmentComputation().getP2p();
        if (input == null || input.getEndpointsObj() == null) {
            LOG.error("Missing End Point Objects. Abort!");
            Uint32 reqID = req.getRp().getRequestId().getValue();
            return MessagesUtil.createErrorMsg(PCEPErrors.END_POINTS_MISSING, reqID);
        }
        VertexKey source = getSourceVertexKey(input.getEndpointsObj());
        VertexKey destination = getDestinationVertexKey(input.getEndpointsObj());
        if (source == null) {
            return MessagesUtil.createNoPathMessage(req.getRp(), MessagesUtil.UNKNOWN_SOURCE);
        }
        if (destination == null) {
            return MessagesUtil.createNoPathMessage(req.getRp(), MessagesUtil.UNKNOWN_DESTINATION);
        }

        /* Create new Constraints Object from the request */
        PathConstraints cts = getConstraints(input, !PSTUtil.isDefaultPST(req.getRp().getTlvs().getPathSetupType()));

        /* Determine Path Computation Algorithm according to Input choice */
        AlgorithmType algoType;
        if ((cts.getTeMetric() == null) && (cts.getDelay() == null)) {
            algoType = AlgorithmType.Spf;
        } else if (cts.getDelay() == null) {
            algoType = AlgorithmType.Cspf;
        } else {
            algoType = AlgorithmType.Samcra;
        }
        PathComputationAlgorithm algo = algoProvider.getPathComputationAlgorithm(tedGraph, algoType);
        if (algo == null) {
            return MessagesUtil.createErrorMsg(PCEPErrors.RESOURCE_LIMIT_EXCEEDED, Uint32.ZERO);
        }

        /* Request Path Computation for given source, destination and constraints */
        LOG.debug("Call Path Computation {} algorithm for path from {} to {} with contraints {}",
                algoType, source, destination, cts);
        final ConstrainedPath cpath = algo.computeP2pPath(source, destination, cts);

        LOG.info("Computed path: {}", cpath.getPathDescription());

        /* Check if we got a valid Path and return appropriate message */
        if (cpath.getStatus() == ComputationStatus.Completed) {
            return MessagesUtil.createPcRepMessage(req.getRp(), req.getSegmentComputation().getP2p(), cpath);
        } else {
            return MessagesUtil.createNoPathMessage(req.getRp(), MessagesUtil.NO_PATH);
        }
    }

    @Override
    public Ero computeEro(EndpointsObj endpoints, Bandwidth bandwidth, ClassType classType, List<Metrics> metrics,
            boolean segmentRouting) {
        VertexKey source = getSourceVertexKey(endpoints);
        VertexKey destination = getDestinationVertexKey(endpoints);
        if (source == null) {
            return null;
        }
        if (destination == null) {
            return null;
        }
        /* Create new Constraints Object from the request */
        PathConstraints cts = getConstraints(endpoints, bandwidth, classType, metrics, segmentRouting);

        /* Determine Path Computation Algorithm according to parameters */
        AlgorithmType algoType;
        if ((cts.getTeMetric() == null) && (cts.getDelay() == null) && (cts.getBandwidth() == null)) {
            algoType = AlgorithmType.Spf;
        } else if (cts.getDelay() == null) {
            algoType = AlgorithmType.Cspf;
        } else {
            algoType = AlgorithmType.Samcra;
        }
        PathComputationAlgorithm algo = algoProvider.getPathComputationAlgorithm(tedGraph, algoType);
        if (algo == null) {
            return null;
        }

        /*
         * Request Path Computation for given source, destination and
         * constraints
         */
        final ConstrainedPath cpath = algo.computeP2pPath(source, destination, cts);

        LOG.info("Computed ERO: {}", cpath.getPathDescription());

        /* Check if we got a valid Path and return appropriate ERO */
        if (cpath.getStatus() == ComputationStatus.Completed) {
            return MessagesUtil.getEro(cpath.getPathDescription());
        } else {
            return null;
        }
    }

    private VertexKey getSourceVertexKey(EndpointsObj endPoints) {
        IpAddress address = null;

        if (endPoints.getAddressFamily() instanceof Ipv4Case) {
            address = new IpAddress(((Ipv4Case) endPoints.getAddressFamily()).getIpv4().getSourceIpv4Address());
        }
        if (endPoints.getAddressFamily() instanceof Ipv6Case) {
            address = new IpAddress(((Ipv6Case) endPoints.getAddressFamily()).getIpv6().getSourceIpv6Address());
        }
        if (address == null) {
            return null;
        }

        ConnectedVertex vertex = tedGraph.getConnectedVertex(address);
        LOG.debug("Compute path from Source {}", vertex.toString());
        return (vertex != null) ? vertex.getVertex().key() : null;
    }

    private VertexKey getDestinationVertexKey(EndpointsObj endPoints) {
        IpAddress address = null;

        if (endPoints.getAddressFamily() instanceof Ipv4Case) {
            address = new IpAddress(((Ipv4Case) endPoints.getAddressFamily()).getIpv4().getDestinationIpv4Address());
        }
        if (endPoints.getAddressFamily() instanceof Ipv6Case) {
            address = new IpAddress(((Ipv6Case) endPoints.getAddressFamily()).getIpv6().getDestinationIpv6Address());
        }
        if (address == null) {
            return null;
        }

        ConnectedVertex vertex = tedGraph.getConnectedVertex(address);
        LOG.debug("Compute path to Destination {}", vertex.toString());
        return (vertex != null) ? vertex.getVertex().key() : null;
    }

    private PathConstraints getConstraints(P2p parameters, boolean segmentRouting) {
        return getConstraints(parameters.getEndpointsObj(), parameters.getBandwidth(), parameters.getClassType(),
                parameters.getMetrics(), segmentRouting);
    }

    private PathConstraints getConstraints(EndpointsObj endpoints, Bandwidth bandwidth, ClassType classType,
            List<Metrics> metrics, boolean segmentRouting) {
        ConstraintsBuilder ctsBuilder = new ConstraintsBuilder();
        Float convert;

        /* Set Metrics if any */
        if (metrics != null) {
            for (Metrics metric : metrics) {
                convert = ByteBuffer.wrap(metric.getMetric().getValue().getValue()).getFloat();
                switch (metric.getMetric().getMetricType().intValue()) {
                    case MessagesUtil.IGP_METRIC:
                        ctsBuilder.setMetric(Uint32.valueOf(convert.longValue()));
                        break;
                    case MessagesUtil.TE_METRIC:
                        ctsBuilder.setTeMetric(Uint32.valueOf(convert.longValue()));
                        break;
                    case MessagesUtil.PATH_DELAY:
                        ctsBuilder.setDelay(new Delay(Uint32.valueOf(convert.longValue())));
                        break;
                    default:
                        LOG.warn("Metric {} is not handle by Path Computation Constraints", metric);
                        break;
                }
            }
        }

        /* Set Bandwidth and Class Type */
        if (bandwidth != null) {
            convert = ByteBuffer.wrap(bandwidth.getBandwidth().getValue()).getFloat();
            ctsBuilder.setBandwidth(new DecimalBandwidth(BigDecimal.valueOf(convert.longValue())));
            if (classType != null) {
                ctsBuilder.setClassType(classType.getClassType().getValue());
            } else {
                ctsBuilder.setClassType(Uint8.ZERO);
            }
        }

        /* Set Address Family */
        if (endpoints.getAddressFamily() instanceof Ipv4Case) {
            if (segmentRouting) {
                ctsBuilder.setAddressFamily(AddressFamily.SrIpv4);
            } else {
                ctsBuilder.setAddressFamily(AddressFamily.Ipv4);
            }
        } else {
            if (segmentRouting) {
                ctsBuilder.setAddressFamily(AddressFamily.SrIpv6);
            } else {
                ctsBuilder.setAddressFamily(AddressFamily.Ipv6);
            }
        }

        return ctsBuilder.build();
    }
}
