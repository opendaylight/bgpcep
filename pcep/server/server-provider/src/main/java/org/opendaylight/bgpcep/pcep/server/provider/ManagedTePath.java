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
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.configured.lsp.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.pcc.configured.lsp.configured.lsp.intended.path.Constraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
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

public class ManagedTePath {

    private ConfiguredLsp cfgLsp = null;
    private final ManagedTeNode teNode;
    private boolean sent = false;
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
        if (lsp.getComputedPath().getPathDescription() == null) {
            return PathStatus.Failed;
        }
        if (!lsp.getComputedPath().getPathDescription().equals(lsp.getComputedPath().getPathDescription())) {
            return newStatus;
        }

        /* All is conform. LSP is in sync with expected result. */
        return PathStatus.Sync;
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

        /*
         * Build Arguments.
         * Note that TE Metric and Delay are not set because, at least, Juniper Routers don't support them.
         */
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .setEndpointsObj(epb.build())
                .setEro(MessagesUtil.getEro(cfgLsp.getComputedPath().getPathDescription()))
                .addAugmentation(new Arguments2Builder()
                        .setLsp(lspBuilder.build())
                        .setPathSetupType(pstBuilder.build())
                        .build());

        /* with Bandwidth and Standard Metric */
        if (iPath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(iPath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
        }
        if (iPath.getConstraints().getMetric() != null) {
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
        if (cfgLsp.getComputedPath().getPathDescription() == null) {
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
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder args;
        args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder()
                .addAugmentation(new Arguments3Builder()
                    .setLsp(lspBuilder.build())
                    .setPathSetupType(pstBuilder.build())
                    .build())
                .setEro(MessagesUtil.getEro(cfgLsp.getComputedPath().getPathDescription()));

        /*  with Bandwidth if defined, but not other Metrics as some routers don't support them */
        if (iPath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(iPath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
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
        if (cfgLsp.getComputedPath().getPathDescription() == null) {
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
