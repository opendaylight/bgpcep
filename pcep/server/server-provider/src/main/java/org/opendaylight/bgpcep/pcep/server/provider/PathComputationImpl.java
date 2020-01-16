/*
 * Copyright (c) 2018 Orange Labs. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server.provider;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.algo.impl.ConstrainedShortestPathFirst;
import org.opendaylight.algo.impl.Samcra;
import org.opendaylight.algo.impl.ShortestPathFirst;
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.get.constrained.path.input.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationImpl implements PathComputation {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationImpl.class);

    private ConnectedGraph tedGraph;

    public PathComputationImpl(ConnectedGraph tedGraph) {
        Preconditions.checkArgument(tedGraph != null);
        this.tedGraph = tedGraph;
    }

    @Override
    public Message computePath(Requests req) {
        LOG.info("Receive Compute Path request");

        /* Check that Request Parameter Object is present */
        if (req == null || req.getRp() == null) {
            LOG.error("Missing Request Parameter Objects. Abort!");
            return MessagesUtil.createErrorMsg(PCEPErrors.RP_MISSING, Uint32.ZERO);
        }

        LOG.debug("Request for path computation {}", req);

        /* Check that mandatory End Point Objects are present and Source / Destination are know in the TED Graph */
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
        PathComputationAlgorithm algo = null;
        if ((cts.getTeMetric() == null) && (cts.getDelay() == null)) {
            algo = new ShortestPathFirst(tedGraph);
        } else if (cts.getDelay() == null) {
            algo = new ConstrainedShortestPathFirst(tedGraph);
        } else {
            algo = new Samcra(tedGraph);
        }

        /* Request Path Computation for given source, destination and constraints */
        final ConstrainedPath cpath = algo.computeP2pPath(source, destination, cts);

        LOG.info("Computed path: {}", cpath.getPathDescription());

        /* Check if we got a valid Path and return appropriate message */
        if (cpath.getStatus() == ComputationStatus.Completed) {
            return MessagesUtil.createPcRepMessage(req.getRp(), req.getSegmentComputation().getP2p(), cpath);
        } else {
            return MessagesUtil.createNoPathMessage(req.getRp(), MessagesUtil.NO_PATH);
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
        return (vertex != null) ? vertex.getVertex().key() : null;
    }

    private PathConstraints getConstraints(P2p parameters, boolean segmentRouting) {
        ConstraintsBuilder ctsBuilder = new ConstraintsBuilder();
        Float convert;

        /* Set Metrics if any */
        for (Metrics metric : parameters.getMetrics()) {
            convert = ByteBuffer.wrap(metric.getMetric().getValue().getValue()).getFloat();
            if (metric.getMetric().getMetricType().intValue() == MessagesUtil.IGP_METRIC) {
                ctsBuilder.setMetric(Uint32.valueOf(convert.longValue()));
            }
            if (metric.getMetric().getMetricType().intValue() == MessagesUtil.TE_METRIC) {
                ctsBuilder.setTeMetric(Uint32.valueOf(convert.longValue()));
            }
            if (metric.getMetric().getMetricType().intValue() == MessagesUtil.PATH_DELAY) {
                ctsBuilder.setDelay(new Delay(Uint32.valueOf(convert.longValue())));
            }
        }
        /* Set Bandwidth and Class Type */
        if (parameters.getBandwidth() != null) {
            convert = ByteBuffer.wrap(parameters.getBandwidth().getBandwidth().getValue()).getFloat();
            ctsBuilder.setBandwidth(BigDecimal.valueOf(convert.longValue()));
            if (parameters.getClassType() != null) {
                ctsBuilder.setClassType(parameters.getClassType().getClassType().getValue());
            } else {
                ctsBuilder.setClassType(Uint8.ZERO);
            }
        }
        /* Set Address Family */
        if (parameters.getEndpointsObj().getAddressFamily() instanceof Ipv4Case) {
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
