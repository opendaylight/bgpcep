/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedEdgeTrigger;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.graph.ConnectedVertexTrigger;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.edge.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.intended.path.Constraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedTePath implements ConnectedEdgeTrigger, ConnectedVertexTrigger {

    private ConfiguredLsp cfgLsp = null;
    private ConfiguredLsp prevLsp = null;
    private final ManagedTeNode teNode;
    private boolean sent = false;
    private boolean triggerFlag = false;
    private PathType type = PathType.Pcc;
    private final InstanceIdentifier<Topology> pcepTopology;
    private final InstanceIdentifier<PathComputationClient1> pccIdentifier;

    private static final Logger LOG = LoggerFactory.getLogger(ManagedTePath.class);

    public ManagedTePath(ManagedTeNode teNode, InstanceIdentifier<Topology> topology) {
        this.teNode = requireNonNull(teNode);
        this.pcepTopology = requireNonNull(topology);
        this.pccIdentifier = pcepTopology.child(Node.class, new NodeKey(teNode.getId())).augmentation(Node1.class)
                .child(PathComputationClient.class).augmentation(PathComputationClient1.class);
    }

    public ManagedTePath(ManagedTeNode teNode, final ConfiguredLsp lsp, InstanceIdentifier<Topology> topology) {
        this.cfgLsp = requireNonNull(lsp);
        this.teNode = requireNonNull(teNode);
        this.pcepTopology = requireNonNull(topology);
        this.pccIdentifier = pcepTopology.child(Node.class, new NodeKey(teNode.getId())).augmentation(Node1.class)
                .child(PathComputationClient.class).augmentation(PathComputationClient1.class);
    }

    public ManagedTePath(ManagedTeNode teNode, final ManagedTePath mngPath) {
        checkArgument(mngPath != null, "Managed TE Path is mandatory. Can't be null or empty!");
        this.cfgLsp = mngPath.getLsp();
        this.sent = mngPath.isSent();
        this.type = mngPath.getType();
        this.teNode = requireNonNull(teNode);
        this.pcepTopology = mngPath.getTopology();
        this.pccIdentifier = pcepTopology.child(Node.class, new NodeKey(teNode.getId())).augmentation(Node1.class)
                .child(PathComputationClient.class).augmentation(PathComputationClient1.class);
    }

    public ConfiguredLsp getLsp() {
        return cfgLsp;
    }

    public ManagedTeNode getManagedTeNode() {
        return teNode;
    }

    public PathType getType() {
        return type;
    }

    public InstanceIdentifier<Topology> getTopology() {
        return pcepTopology;
    }

    public ManagedTePath setConfiguredLsp(final ConfiguredLsp lsp) {
        this.prevLsp = this.cfgLsp;
        this.cfgLsp = lsp;
        return this;
    }

    public ManagedTePath setType(PathType type) {
        this.type = type;
        return this;
    }

    /**
     * Mark this TE Path as synchronized and update the Data Store accordingly.
     *
     */
    public void sync() {
        this.cfgLsp = new ConfiguredLspBuilder(cfgLsp).setPathStatus(PathStatus.Sync).build();
        updateToDataStore();
    }

    /**
     * Disabling this TE Path by marking it as Configured. Do not update the Data Store.
     */
    public void disabled() {
        this.cfgLsp = new ConfiguredLspBuilder(cfgLsp).setPathStatus(PathStatus.Configured).build();
    }

    /**
     * Mark this TE Path as Failed.
     */
    public void failed() {
        this.cfgLsp = new ConfiguredLspBuilder(cfgLsp).setPathStatus(PathStatus.Failed).build();
        updateToDataStore();
    }

    public boolean isSent() {
        return sent;
    }

    /**
     * Compare the current TE Path against the reported LSP to determine if there are in Sync, need Update or
     * considered as in Failure if already updated.
     *
     * @param lsp   LSP that corresponds to the reported LSP
     *
     * @return      new LSP status
     */
    public PathStatus checkReportedPath(final ConfiguredLsp lsp) {
        /* Check if this path has not been already verified */
        if (cfgLsp.getPathStatus() == PathStatus.Sync) {
            return PathStatus.Sync;
        }

        /* Check Source, Destination and routing method which must be the same */
        final IntendedPath iPath = lsp.getIntendedPath();
        final IntendedPath rPath = lsp.getIntendedPath();
        PathStatus newStatus = sent ? PathStatus.Failed : PathStatus.Updated;
        if (!iPath.getSource().equals(rPath.getSource()) || !iPath.getDestination().equals(rPath.getDestination())) {
            return PathStatus.Failed;
        }

        /* Check constraints ... */
        final Constraints icts = iPath.getConstraints();
        final Constraints rcts = rPath.getConstraints();
        if (!icts.getAddressFamily().equals(rcts.getAddressFamily())) {
            return newStatus;
        }
        if (icts.getBandwidth() != null && !icts.getBandwidth().equals(rcts.getBandwidth())) {
            return newStatus;
        }
        /*
         * ClassType, TE metric and Delay are not supported by all routers: need
         * to check them only if there are all present
         */
        if (icts.getClassType() != null && rcts.getClassType() != null
                        && !icts.getClassType().equals(rcts.getClassType())) {
            return newStatus;
        }
        if (icts.getTeMetric() != null && rcts.getTeMetric() != null
                && !icts.getTeMetric().equals(rcts.getTeMetric())) {
            return newStatus;
        }
        if (icts.getDelay() != null && rcts.getDelay() != null && !icts.getDelay().equals(rcts.getDelay())) {
            return newStatus;
        }

        /* ... and Path to determine if an update is required */
        if (lsp.getComputedPath().getComputationStatus() != ComputationStatus.Completed) {
            return PathStatus.Failed;
        }
        if (!lsp.getComputedPath().getPathDescription().equals(lsp.getComputedPath().getPathDescription())) {
            return newStatus;
        }

        /* All is conform. LSP is in sync with expected result. */
        return PathStatus.Sync;
    }

    private void configureGraph(ConnectedGraph graph, ComputedPath cpath, Constraints cts, boolean config) {
        /* Check that Connected Graph is valid */
        if (graph == null) {
            return;
        }

        /* Verify that we have a valid Computed Path and that the LSP is in SYNC */
        if (cpath.getComputationStatus() != ComputationStatus.Completed || cfgLsp.getPathStatus() != PathStatus.Sync) {
            return;
        }

        /* Loop the path description to add reserved bandwidth and triggers for this LSP */
        final Long bw = cts.getBandwidth() == null ? 0L : cts.getBandwidth().getValue().longValue();
        int cos = cts.getClassType() == null ? 0 : cts.getClassType().intValue();
        final AddressFamily af = cts.getAddressFamily();
        final String lspId = teNode.getId().getValue() + "/" + cfgLsp.getName();
        ConnectedEdge edge = null;
        for (PathDescription path : cpath.getPathDescription()) {
            edge = null;
            switch (af) {
                case Ipv4:
                case SrIpv4:
                    if (path.getIpv4() != null) {
                        edge = graph.getConnectedEdge(new IpAddress(path.getIpv4()));
                    } else if (path.getRemoteIpv4() != null) {
                        edge = graph.getConnectedEdge(new IpAddress(path.getRemoteIpv4()));
                        if (edgeAttrNotNull(edge)) {
                            edge = graph.getConnectedEdge(edge.getEdge().getEdgeAttributes().getRemoteAddress());
                        }
                    }
                    break;
                case Ipv6:
                case SrIpv6:
                    if (path.getIpv6() != null) {
                        edge = graph.getConnectedEdge(new IpAddress(path.getIpv6()));
                    } else if (path.getRemoteIpv6() != null) {
                        edge = graph.getConnectedEdge(new IpAddress(path.getRemoteIpv6()));
                        if (edgeAttrNotNull(edge)) {
                            /* Need to force using IPv6 address as Connected Edge is searched first on IPv4 address */
                            edge = graph.getConnectedEdge(new IpAddress(
                                    edge.getEdge().getEdgeAttributes().getRemoteAddress6()));
                        }
                    }
                    break;
                default:
                    break;
            }

            if (edge == null) {
                continue;
            }

            if (config) {
                if (bw != 0L) {
                    edge.addBandwidth(bw, cos);
                }
                edge.registerTrigger(this, lspId);
                if (edge.getSource() != null) {
                    edge.getSource().registerTrigger(this, lspId);
                }
            } else {
                if (bw != 0L) {
                    edge.delBandwidth(bw, cos);
                }
                edge.unRegisterTrigger(this, lspId);
                if (edge.getSource() != null) {
                    edge.getSource().unRegisterTrigger(this, lspId);
                }
            }
        }
        if (edge != null && edge.getDestination() != null) {
            if (config) {
                edge.getDestination().registerTrigger(this, lspId);
            } else {
                edge.getDestination().unRegisterTrigger(this, lspId);
            }
        }

        /* Finally, reset Trigger Flag to activate them */
        triggerFlag = false;
    }

    private boolean edgeAttrNotNull(ConnectedEdge edge) {
        return edge != null && edge.getEdge() != null && edge.getEdge().getEdgeAttributes() != null;
    }

    public void setGraph(ConnectedGraph graph) {
        configureGraph(graph, cfgLsp.getComputedPath(), cfgLsp.getIntendedPath().getConstraints(), true);
    }

    public void unsetGraph(ConnectedGraph graph) {
        configureGraph(graph, cfgLsp.getComputedPath(), cfgLsp.getIntendedPath().getConstraints(), false);
    }

    public void updateGraph(ConnectedGraph graph) {
        /* First unset Bandwidth and Triggers for the old path if any */
        if (prevLsp != null) {
            configureGraph(graph, prevLsp.getComputedPath(), prevLsp.getIntendedPath().getConstraints(), false);
        }

        /* Then add Bandwidth and Triggers for the current path */
        configureGraph(graph, cfgLsp.getComputedPath(), cfgLsp.getIntendedPath().getConstraints(), true);

        /* And memorize current LSP for latter update */
        prevLsp = cfgLsp;
    }

    /**
     * Reset Triggered Flag.
     */
    public void unSetTriggerFlag() {
        triggerFlag = false;
    }

    @Override
    public boolean verifyVertex(@Nullable ConnectedVertex next, @Nullable Vertex current) {
        /* Check if there is an on-going trigger */
        if (triggerFlag) {
            return false;
        }

        /* Check if Vertex has been removed */
        Vertex vertex = next.getVertex();
        if (vertex == null) {
            triggerFlag = true;
            return true;
        }

        /* Check if Vertex changed its Segment Routing Global Block */
        final AddressFamily af = cfgLsp.getIntendedPath().getConstraints().getAddressFamily();
        if ((af == AddressFamily.SrIpv4 || af == AddressFamily.SrIpv6) && !current.getSrgb().equals(vertex.getSrgb())) {
            LOG.debug("Vertex {} modified its SRGB {} / {}", vertex.getName(), current.getSrgb(), vertex.getSrgb());
            triggerFlag = true;
            return true;
        }

        /* All is fine */
        triggerFlag = false;
        return false;
    }

    @Override
    public boolean verifyEdge(@Nullable ConnectedEdge next, @Nullable Edge current) {
        /* Check if there is an on-going trigger */
        if (triggerFlag) {
            return false;
        }

        /* Check if Edge or Attributes has been removed */
        Edge edge = next.getEdge();
        if (edge == null || edge.getEdgeAttributes() == null) {
            triggerFlag = true;
            return true;
        }

        /* Check that Configured LSP has valid constraints */
        final Constraints constraints = cfgLsp.getIntendedPath().getConstraints();
        if (constraints == null) {
            return false;
        }

        /* Check if Metric is always met */
        Long metric = 0L;
        Long delta = 0L;
        if (constraints.getDelay() != null) {
            if (edge.getEdgeAttributes().getDelay() != null) {
                metric = constraints.getDelay().getValue().longValue();
                delta = edge.getEdgeAttributes().getDelay().getValue().longValue()
                        - current.getEdgeAttributes().getDelay().getValue().longValue();
            } else {
                triggerFlag = true;
                return true;
            }
        }
        if (constraints.getTeMetric() != null) {
            if (edge.getEdgeAttributes().getTeMetric() != null) {
                metric = constraints.getTeMetric().longValue();
                delta = edge.getEdgeAttributes().getTeMetric().longValue()
                        - current.getEdgeAttributes().getTeMetric().longValue();
            } else {
                triggerFlag = true;
                return true;
            }
        } else if (constraints.getMetric() != null) {
            if (edge.getEdgeAttributes().getMetric() != null) {
                metric = constraints.getMetric().longValue();
                delta = edge.getEdgeAttributes().getMetric().longValue()
                        - current.getEdgeAttributes().getMetric().longValue();
            } else {
                triggerFlag = true;
                return true;
            }
        }
        if (metric != 0L && cfgLsp.getComputedPath().getComputedMetric() != null
                && cfgLsp.getComputedPath().getComputedMetric().longValue() + delta > metric) {
            LOG.debug("Following an update on Edge {} Metric is no longer guaranteed: {} / {}",
                    edge.getName(),
                    cfgLsp.getComputedPath().getComputedMetric().longValue() + delta,
                    metric);
            triggerFlag = true;
            return true;
        }

        /* Check if Bandwidth is always met */
        if (constraints.getBandwidth() != null) {
            if (edge.getEdgeAttributes().getMaxLinkBandwidth() == null
                    || edge.getEdgeAttributes().getMaxResvLinkBandwidth() == null
                    || edge.getEdgeAttributes().getUnreservedBandwidth() == null) {
                triggerFlag = true;
                return true;
            }
            Long bandwidth = constraints.getBandwidth().getValue().longValue();
            Long unrsv = 0L;
            int cos = 0;
            for (UnreservedBandwidth unResBw : edge.getEdgeAttributes().getUnreservedBandwidth()) {
                if (unResBw.getClassType().intValue() == cos) {
                    unrsv = unResBw.getBandwidth().getValue().longValue();
                    break;
                }
            }
            Long maxBW = edge.getEdgeAttributes().getMaxLinkBandwidth().getValue().longValue();
            if (bandwidth > List.of(
                    unrsv,
                    /* maxBW might be on the list but will always be greater than the next items */
                    maxBW - next.getCosResvBandwidth(cos),
                    maxBW - next.getGlobalResvBandwidth(),
                    edge.getEdgeAttributes().getMaxResvLinkBandwidth().getValue().longValue())
                    .stream().mapToLong(v -> v)
                    .min().getAsLong()
            ) {
                LOG.debug("Following an update on Edge {}, Reserved bandwidth is no longer guaranteed", edge.getName());
                triggerFlag = true;
                return true;
            }
        }

        /* Check if Edge changed its Adjacency SID */
        final AddressFamily af = cfgLsp.getIntendedPath().getConstraints().getAddressFamily();
        if ((af == AddressFamily.SrIpv4 || af == AddressFamily.SrIpv6)
                && !current.getEdgeAttributes().getAdjSid().equals(edge.getEdgeAttributes().getAdjSid())) {
            LOG.debug("Edge {} has modified its Adjacency SID", edge.getName());
            triggerFlag = true;
            return true;
        }

        return false;
    }

    /**
     * Convert LSP as Add LSP Input class to enforce it into the PCC through a call to add-lsp RPCs.
     *
     * @return  new Add LSP Input
     */
    private AddLspInput getAddLspInput() {
        /* Create EndPoint Object */
        final IntendedPath iPath = cfgLsp.getIntendedPath();
        final EndpointsObjBuilder epb = new EndpointsObjBuilder()
                .setIgnore(false)
                .setProcessingRule(true);
        if (iPath.getSource().getIpv4Address() != null) {
            final Ipv4Builder ipBuilder = new Ipv4Builder()
                    .setDestinationIpv4Address(new Ipv4AddressNoZone(iPath.getDestination().getIpv4Address()))
                    .setSourceIpv4Address(new Ipv4AddressNoZone(iPath.getSource().getIpv4Address()));
            epb.setAddressFamily((new Ipv4CaseBuilder().setIpv4(ipBuilder.build()).build()));
        } else if (cfgLsp.getIntendedPath().getSource().getIpv6Address() != null) {
            final Ipv6Builder ipBuilder = new Ipv6Builder()
                    .setDestinationIpv6Address(new Ipv6AddressNoZone(iPath.getDestination().getIpv6Address()))
                    .setSourceIpv6Address(new Ipv6AddressNoZone(iPath.getSource().getIpv6Address()));
            epb.setAddressFamily((new Ipv6CaseBuilder().setIpv6(ipBuilder.build()).build()));
        } else {
            // In case of ...
            return null;
        }

        /* Create Path Setup Type */
        final PathSetupTypeBuilder pstBuilder = new PathSetupTypeBuilder();
        switch (iPath.getConstraints().getAddressFamily()) {
            case SrIpv4:
            case SrIpv6:
                pstBuilder.setPst(Uint8.ONE);
                break;
            default:
                pstBuilder.setPst(Uint8.ZERO);
                break;
        }

        /* Create LSP */
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        /* Build Arguments. */
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .setEndpointsObj(epb.build())
                .setEro(MessagesUtil.getEro(cfgLsp.getComputedPath().getPathDescription()))
                .addAugmentation(new Arguments2Builder()
                        .setLsp(lspBuilder.build())
                        .setPathSetupType(pstBuilder.build())
                        .build());

        /* with Bandwidth and Metric if defined */
        if (iPath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(iPath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
        }
        /* Note that Delay are not set because, at least, Juniper Routers don't support them */
        if (iPath.getConstraints().getTeMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder()
                    .setComputed(true)
                    .setMetricType(Uint8.TWO)
                    .setValue(new Float32(ByteBuffer.allocate(4)
                            .putFloat(iPath.getConstraints().getTeMetric().floatValue()).array()));
            args.setMetrics(Collections.singletonList(new MetricsBuilder().setMetric(metricBuilder.build()).build()));
        } else if (iPath.getConstraints().getMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder()
                    .setComputed(true)
                    .setMetricType(Uint8.ONE)
                    .setValue(new Float32(
                            ByteBuffer.allocate(4).putFloat(iPath.getConstraints().getMetric().floatValue()).array()));
            args.setMetrics(Collections.singletonList(new MetricsBuilder().setMetric(metricBuilder.build()).build()));
        }

        /*
         * NOTE: Seems that ClassType is not supported by some routers. Skip it for the moment.
         */
        /* With Class Type if defined
        if (iPath.getConstraints().getClassType() != null) {
            args.setClassType(
                new ClassTypeBuilder()
                    .setClassType(new ClassType(iPath.getConstraints().getClassType()))
                    .setIgnore(false)
                    .setProcessingRule(true)
                    .build());
        }
        */

        /* Finally, build addLSP input */
        return new AddLspInputBuilder()
                .setNode(teNode.getId())
                .setName(cfgLsp.getName())
                .setArguments(args.build())
                .setNetworkTopologyRef(new NetworkTopologyRef(pcepTopology))
                .build();
    }


    /**
     * Call add-lsp RPC to enforce the LSP into the PCC. This action will trigger a PcInitiate message to the PCC.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Add LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<AddLspOutput>> addPath(final NetworkTopologyPcepService ntps) {
        /* Check if we could add this path */
        if ((type != PathType.Initiated) || !teNode.isSync()) {
            return null;
        }

        /* Check if we have a valid Path */
        if (cfgLsp.getComputedPath().getComputationStatus() != ComputationStatus.Completed) {
            return null;
        }

        sent = true;
        final ListenableFuture<RpcResult<AddLspOutput>> enforce = ntps.addLsp(getAddLspInput());
        LOG.info("Call Add LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<AddLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<AddLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Enforce LSP success {}", result.getResult());
                } else {
                    LOG.debug("Unable to enforce LSP {} on Node {}: Got error {}", cfgLsp.getName(), teNode.getId(),
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed enforce LSP {} on Node {}", cfgLsp.getName(), teNode.getId());
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    /**
     * Convert LSP as Update LSP Input class to update it into the PCC through a call to update-lsp RPCs.
     *
     * @return  new Update LSP Input
     */
    private UpdateLspInput getUpdateLspInput() {
        /* Create Path Setup Type */
        final IntendedPath iPath = cfgLsp.getIntendedPath();
        final PathSetupTypeBuilder pstBuilder = new PathSetupTypeBuilder();
        switch (iPath.getConstraints().getAddressFamily()) {
            case SrIpv4:
            case SrIpv6:
                pstBuilder.setPst(Uint8.ONE);
                break;
            default:
                pstBuilder.setPst(Uint8.ZERO);
                break;
        }

        /* Create LSP */
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        /* Build Arguments */
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730
            .update.lsp.args.ArgumentsBuilder args;
        args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730
            .update.lsp.args.ArgumentsBuilder()
                .addAugmentation(new Arguments3Builder()
                    .setLsp(lspBuilder.build())
                    .setPathSetupType(pstBuilder.build())
                    .build())
                .setEro(MessagesUtil.getEro(cfgLsp.getComputedPath().getPathDescription()));

        /*  with Bandwidth and Metric if defined */
        if (iPath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(iPath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
        }
        /* Note that Delay are not set because, at least, Juniper Routers don't support them */
        if (iPath.getConstraints().getTeMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder()
                    .setComputed(true)
                    .setMetricType(Uint8.TWO)
                    .setValue(new Float32(ByteBuffer.allocate(4)
                            .putFloat(iPath.getConstraints().getTeMetric().floatValue()).array()));
            args.setMetrics(Collections.singletonList(new MetricsBuilder().setMetric(metricBuilder.build()).build()));
        } else if (iPath.getConstraints().getMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder()
                    .setComputed(true)
                    .setMetricType(Uint8.ONE)
                    .setValue(new Float32(ByteBuffer.allocate(4)
                            .putFloat(iPath.getConstraints().getMetric().floatValue()).array()));
            args.setMetrics(Collections.singletonList(new MetricsBuilder().setMetric(metricBuilder.build()).build()));
        }

        /*
         * NOTE: Seems that ClassType is not supported by some routers. Skip it for the moment.
         */
        /* With Class Type if defined
        if (iPath.getConstraints().getClassType() != null) {
            args.setClassType(
                new ClassTypeBuilder()
                    .setClassType(new ClassType(iPath.getConstraints().getClassType()))
                    .setIgnore(false)
                    .setProcessingRule(true)
                    .build());
        }
        */

        /* Finally, build updateLSP input */
        return new UpdateLspInputBuilder()
                .setNode(teNode.getId())
                .setName(cfgLsp.getName())
                .setArguments(args.build())
                .setNetworkTopologyRef(new NetworkTopologyRef(pcepTopology))
                .build();
    }

    /**
     * Call update-lsp RPC to enforce the LSP into the PCC. This action will trigger a PcUpdate message to the PCC.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Update LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<UpdateLspOutput>> updatePath(final NetworkTopologyPcepService ntps) {

        /* Check if we could update this path */
        if ((type != PathType.Initiated && type != PathType.Delegated) || !teNode.isSync()) {
            return null;
        }

        /* Check if we have a valid ERO */
        if (cfgLsp.getComputedPath().getComputationStatus() != ComputationStatus.Completed) {
            return null;
        }

        sent = true;
        final NodeId id = teNode.getId();
        final ListenableFuture<RpcResult<UpdateLspOutput>> enforce = ntps.updateLsp(getUpdateLspInput());
        LOG.info("Call Update LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<UpdateLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<UpdateLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Update LSP success {}", result.getResult());
                } else {
                    LOG.debug("Unable to update LSP {} on Node {}: Got error {}", cfgLsp.getName(), id,
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed update LSP {} on Node {}", cfgLsp.getName(), id);
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    /**
     * Call remove-lsp RPC to remove the LSP from the PCC. This action will trigger a PcInitiate message to the PCC
     * with 'R' bit set.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Remove LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<RemoveLspOutput>> removePath(final NetworkTopologyPcepService ntps) {

        /* Check if we could remove this path */
        if ((type != PathType.Initiated) || !teNode.isSync() || cfgLsp.getPathStatus() != PathStatus.Sync) {
            return null;
        }

        sent = true;
        final NodeId id = teNode.getId();
        final RemoveLspInput rli = new RemoveLspInputBuilder()
                .setNode(id)
                .setName(cfgLsp.getName())
                .setNetworkTopologyRef(new NetworkTopologyRef(pcepTopology))
                .build();
        final ListenableFuture<RpcResult<RemoveLspOutput>> enforce = ntps.removeLsp(rli);
        LOG.info("Call Remove LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<RemoveLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<RemoveLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Delete LSP success {}", result.getResult());
                } else {
                    LOG.debug("Unable to delete LSP {} on Node {}: Got error {}", cfgLsp.getName(), id,
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed delete LSP {} on Node {}", cfgLsp.getName(), id);
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    /**
     * Add LSP components to the Operational Data Store.
     */
    public synchronized void addToDataStore() {
        /* Check if we could add this path */
        if (!teNode.isSync()) {
            return;
        }

        final WriteTransaction trans = teNode.getTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, pccIdentifier.child(ConfiguredLsp.class, cfgLsp.key()), cfgLsp);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Configured LSP {} has been published in operational datastore ", cfgLsp.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot write Configured LSP {} to the operational datastore (transaction: {})",
                        cfgLsp.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Update LSP components to the Data Store.
     */
    public synchronized void updateToDataStore() {
        /* Check if we could update this path */
        if (!teNode.isSync()) {
            return;
        }

        final WriteTransaction trans = teNode.getTransaction();
        trans.merge(LogicalDatastoreType.OPERATIONAL, pccIdentifier.child(ConfiguredLsp.class, cfgLsp.key()), cfgLsp);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Configured LSP {} has been updated in operational datastore ", cfgLsp.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot update Configured LSP {} to the operational datastore (transaction: {})",
                        cfgLsp.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Remove LSP components to the Data Store.
     */
    public synchronized void removeFromDataStore() {
        /* Check if we could remove this path */
        if (!teNode.isSync()) {
            return;
        }

        final WriteTransaction trans = teNode.getTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, pccIdentifier.child(ConfiguredLsp.class, cfgLsp.key()));
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Configured LSP {} has been deleted in operational datastore ", cfgLsp.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot delete Configured LSP {} from the operational datastore (transaction: {})",
                        cfgLsp.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper("ManagedTePath");
        CodeHelpers.appendValue(helper, "ConfiguredLsp", cfgLsp);
        CodeHelpers.appendValue(helper, "PathType", type);
        CodeHelpers.appendValue(helper, "Sent", sent);
        return helper.toString();
    }

}
