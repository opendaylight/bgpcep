/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AlgorithmType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.PathConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.get.constrained.path.input.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.ExcludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.ExcludeRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.IncludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.IncludeRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationImpl implements PathComputation {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationImpl.class);

    private final ConnectedGraph tedGraph;
    private final PathComputationProvider algoProvider;

    public PathComputationImpl(final ConnectedGraph tedGraph, final PathComputationProvider algoProvider) {
        this.tedGraph = requireNonNull(tedGraph);
        this.algoProvider = requireNonNull(algoProvider);
    }

    @Override
    public Message computePath(final Requests req) {
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
        if (cts.getDelay() != null) {
            algoType = AlgorithmType.Samcra;
        } else if (cts.getTeMetric() != null) {
            algoType = AlgorithmType.Cspf;
        } else {
            algoType = AlgorithmType.Spf;
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

    public ComputedPath computeTePath(final IntendedPath intend) {
        final ComputedPathBuilder cpb = new ComputedPathBuilder();
        ConnectedVertex source = tedGraph.getConnectedVertex(intend.getSource());
        ConnectedVertex destination = tedGraph.getConnectedVertex(intend.getDestination());

        if (source == null) {
            return cpb.setComputationStatus(ComputationStatus.NoSource).build();
        }
        if (destination == null) {
            return cpb.setComputationStatus(ComputationStatus.NoDestination).build();
        }

        /* Determine Path Computation Algorithm according to parameters */
        AlgorithmType algoType;
        if (intend.getConstraints().getDelay() != null) {
            algoType = AlgorithmType.Samcra;
        } else if (intend.getConstraints().getTeMetric() != null) {
            algoType = AlgorithmType.Cspf;
        } else {
            algoType = AlgorithmType.Spf;
        }
        PathComputationAlgorithm algo = algoProvider.getPathComputationAlgorithm(tedGraph, algoType);
        if (algo == null) {
            return cpb.setComputationStatus(ComputationStatus.Failed).build();
        }

        /* Request Path Computation for given source, destination and constraints */
        final ConstrainedPath cpath = algo.computeP2pPath(source.getVertex().key(), destination.getVertex().key(),
                intend.getConstraints());

        LOG.info("Computed path: {}", cpath.getPathDescription());

        /* Check if we got a valid Path and return appropriate Path Description */
        if (cpath.getStatus() == ComputationStatus.Completed) {
            cpb.setPathDescription(cpath.getPathDescription()).setComputationStatus(ComputationStatus.Completed);
            if (intend.getConstraints().getDelay() != null) {
                cpb.setComputedMetric(cpath.getDelay().getValue());
            } else if (intend.getConstraints().getTeMetric() != null) {
                cpb.setComputedMetric(cpath.getTeMetric());
            } else {
                cpb.setComputedMetric(cpath.getMetric());
            }
            return cpb.build();
        } else {
            return cpb.setComputationStatus(ComputationStatus.NoPath).build();
        }
    }

    @Override
    public Ero computeEro(final EndpointsObj endpoints, final Bandwidth bandwidth, final ClassType classType,
            final List<Metrics> metrics, final Xro xro, final Iro iro, final boolean segmentRouting) {
        /* Get source and destination Vertex and verify there are valid */
        VertexKey source = getSourceVertexKey(endpoints);
        if (source == null) {
            return null;
        }
        VertexKey destination = getDestinationVertexKey(endpoints);
        if (destination == null) {
            return null;
        }
        /* Create new Constraints Object from the request */
        PathConstraints cts = getConstraints(endpoints, bandwidth, classType, metrics, xro, iro, segmentRouting);

        /* Determine Path Computation Algorithm according to parameters */
        AlgorithmType algoType;
        if (cts.getDelay() != null) {
            algoType = AlgorithmType.Samcra;
        } else if (cts.getTeMetric() != null) {
            algoType = AlgorithmType.Cspf;
        } else {
            algoType = AlgorithmType.Spf;
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

        LOG.info("Computed path: {}", cpath.getPathDescription());

        /* Check if we got a valid Path and return appropriate ERO */
        if (cpath.getStatus() == ComputationStatus.Completed) {
            return MessagesUtil.getEro(cpath.getPathDescription());
        } else {
            return null;
        }
    }

    private VertexKey getSourceVertexKey(final EndpointsObj endPoints) {
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
        LOG.debug("Compute path from Source {}", vertex != null ? vertex : "Unknown");
        return vertex != null ? vertex.getVertex().key() : null;
    }

    private VertexKey getDestinationVertexKey(final EndpointsObj endPoints) {
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
        LOG.debug("Compute path to Destination {}", vertex != null ? vertex : "Unknown");
        return vertex != null ? vertex.getVertex().key() : null;
    }

    /* Convert Exclude Route Object (list of IP prefix) into Exclude Route (list of IP address) */
    private static List<ExcludeRoute> getExcludeRoute(final Xro xro, AddressFamily af) {
        if (xro == null || xro.getSubobject() == null || xro.getSubobject().isEmpty()) {
            return null;
        }
        ArrayList<ExcludeRoute> erl = new ArrayList<ExcludeRoute>();
        for (int i = 0; i < xro.getSubobject().size(); i++) {
            final SubobjectType sbt = xro.getSubobject().get(i).getSubobjectType();
            if (sbt instanceof IpPrefixCase) {
                final IpPrefixCase ipc = (IpPrefixCase) sbt;
                switch (af) {
                    case Ipv4:
                    case SrIpv4:
                        erl.add(new ExcludeRouteBuilder().setIpv4(new Ipv4Address(
                                ipc.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue().split("/")[0]))
                                .build());
                        break;
                    case Ipv6:
                    case SrIpv6:
                        erl.add(new ExcludeRouteBuilder().setIpv6(new Ipv6Address(
                                ipc.getIpPrefix().getIpPrefix().getIpv6Prefix().getValue().split("/")[0]))
                                .build());
                        break;
                    default:
                        break;
                }
            }
        }
        return erl;
    }

    /* Convert Include Route Object (list of IP prefix) into Exclude Route (list of IP address) */
    private static List<IncludeRoute> getIncludeRoute(final Iro iro, AddressFamily af) {
        if (iro == null || iro.getSubobject() == null || iro.getSubobject().isEmpty()) {
            return null;
        }
        ArrayList<IncludeRoute> irl = new ArrayList<IncludeRoute>();
        for (int i = 0; i < iro.getSubobject().size(); i++) {
            final SubobjectType sbt = iro.getSubobject().get(i).getSubobjectType();
            if (sbt instanceof IpPrefixCase) {
                final IpPrefixCase ipc = (IpPrefixCase) sbt;
                switch (af) {
                    case Ipv4:
                    case SrIpv4:
                        irl.add(new IncludeRouteBuilder().setIpv4(new Ipv4Address(
                                ipc.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue().split("/")[0]))
                                .build());
                        break;
                    case Ipv6:
                    case SrIpv6:
                        irl.add(new IncludeRouteBuilder().setIpv6(new Ipv6Address(
                                ipc.getIpPrefix().getIpPrefix().getIpv6Prefix().getValue().split("/")[0]))
                                .build());
                        break;
                    default:
                        break;
                }
            }
        }
        return irl;
    }

    private static PathConstraints getConstraints(final P2p parameters, final boolean segmentRouting) {
        return getConstraints(parameters.getEndpointsObj(), parameters.getBandwidth(), parameters.getClassType(),
                parameters.getMetrics(), parameters.getXro(), parameters.getIro(), segmentRouting);
    }

    private static PathConstraints getConstraints(final EndpointsObj endpoints, final Bandwidth bandwidth,
            final ClassType classType, final List<Metrics> metrics, final Xro xro, final Iro iro,
            final boolean segmentRouting) {
        ConstraintsBuilder ctsBuilder = new ConstraintsBuilder();

        /* Set Metrics if any */
        if (metrics != null) {
            for (Metrics metric : metrics) {
                Float convert = ByteBuffer.wrap(metric.getMetric().getValue().getValue()).getFloat();
                final long value = convert.longValue();
                /* Skip Metric with value equal to 0 */
                if (value == 0) {
                    continue;
                }

                switch (metric.getMetric().getMetricType().intValue()) {
                    case MessagesUtil.IGP_METRIC:
                        ctsBuilder.setMetric(Uint32.valueOf(value));
                        break;
                    case MessagesUtil.TE_METRIC:
                        ctsBuilder.setTeMetric(Uint32.valueOf(value));
                        break;
                    case MessagesUtil.PATH_DELAY:
                        ctsBuilder.setDelay(new Delay(Uint32.valueOf(value)));
                        break;
                    default:
                        LOG.warn("Metric {} is not handle by Path Computation Constraints", metric);
                        break;
                }
            }
        }

        /* Set Bandwidth and Class Type */
        if (bandwidth != null) {
            Float convert = ByteBuffer.wrap(bandwidth.getBandwidth().getValue()).getFloat();
            final long value = convert.longValue();
            /* Skip Bandwidth with value equal to 0 */
            if (value != 0) {
                // FIXME: correct rounding/truncation!
                ctsBuilder.setBandwidth(new DecimalBandwidth(Decimal64.valueOf(2, value)));
                if (classType != null) {
                    ctsBuilder.setClassType(classType.getClassType().getValue());
                }
            }
        }

        AddressFamily af = endpoints.getAddressFamily() instanceof Ipv4Case
                ? segmentRouting ? AddressFamily.SrIpv4 : AddressFamily.Ipv4
                : segmentRouting ? AddressFamily.SrIpv6 : AddressFamily.Ipv6;

        /* Set Address Family, Exclude Route and Include Route if any */
        return ctsBuilder
                .setAddressFamily(af)
                .setExcludeRoute(getExcludeRoute(xro, af))
                .setIncludeRoute(getIncludeRoute(iro, af))
                .build();
    }
}
