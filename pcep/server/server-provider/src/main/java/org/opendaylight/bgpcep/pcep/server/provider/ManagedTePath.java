/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.ManagedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.ManagedNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.TePathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.ActualPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.IntendedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev210720.managed.path.managed.node.te.path.intended.path.Constraints;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedTePath {

    private TePath path = null;
    private boolean sent = false;
    private final ManagedTeNode teNode;
    private static final InstanceIdentifier<ManagedPath> MANAGED_PATH_IDENTIFIER =
            InstanceIdentifier.builder(ManagedPath.class).build();

    private static final Logger LOG = LoggerFactory.getLogger(ManagedTePath.class);

    public ManagedTePath(ManagedTeNode teNode) {
        this.teNode = teNode;
    }

    public ManagedTePath(ManagedTeNode teNode, final TePath path) {
        this.path = path;
        this.teNode = teNode;
    }

    public ManagedTePath(ManagedTeNode teNode, final ManagedTePath mngPath) {
        this.path = mngPath.getPath();
        this.sent = mngPath.isSent();
        this.teNode = teNode;
    }

    public TePath getPath() {
        return path;
    }

    public ManagedTeNode getManagedTeNode() {
        return teNode;
    }

    public ManagedTePath setPath(final TePath path) {
        if (this.path == null) {
            this.path = path;
            addToDataStore();
        } else {
            this.path = path;
            updateToDataStore();
        }
        return this;
    }

    /**
     * Mark this TE Path as synchronized and update status of Actual path and update the Data Store accordingly.
     *
     * @param status    Status of the Actual path
     */
    public void sync(OperationalStatus status) {
        final TePath newPath = new TePathBuilder(path)
                .setActualPath(new ActualPathBuilder(path.getActualPath()).setStatus(status).build())
                .setPathStatus(PathStatus.Sync)
                .build();
        this.path = newPath;
        updateToDataStore();
    }

    /**
     * Disabling this TE Path by marking it as Configured. Do not update the Data Store.
     */
    public void disabled() {
        final TePath newPath = new TePathBuilder(path).setPathStatus(PathStatus.Configured).build();
        this.path = newPath;
    }

    /**
     * Mark this TE Path as Failed and update the Data Store accordingly.
     */
    public void failed() {
        final TePath newPath = new TePathBuilder(path).setPathStatus(PathStatus.Failed).build();
        this.path = newPath;
        updateToDataStore();
    }

    public boolean isSent() {
        return sent;
    }

    /**
     * Compare the current TE Path against the reported TE Path to determine if there are in Sync, need Update or
     * considered as in Failure if already updated.
     *
     * @param tePath    TE Path that corresponds to the reported LSP
     *
     * @return          new TE Path status
     */
    public PathStatus checkReportedPath(final TePath tePath) {
        /* Check if this path has not been already verified */
        if (path.getPathStatus() == PathStatus.Sync) {
            return PathStatus.Sync;
        }

        /* Check Source, Destination and routing method which must be the same */
        final IntendedPath iPath = path.getIntendedPath();
        final IntendedPath rPath = tePath.getIntendedPath();
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
        if (tePath.getActualPath().getPathDescription() == null) {
            return PathStatus.Failed;
        }
        if (!path.getActualPath().getPathDescription().equals(tePath.getActualPath().getPathDescription())) {
            return newStatus;
        }

        /* All is conform. TE Path is in sync with expected result. */
        return PathStatus.Sync;
    }

    /**
     * Convert TE Path as Add LSP Input class to enforce it into the PCC through a call to add-lsp RPCs.
     *
     * @return  new Add LSP Input
     */
    private AddLspInput getAddLspInput(final InstanceIdentifier<Topology> topologyRef) {
        // Create EndPoint Object
        final IntendedPath iPath = path.getIntendedPath();
        final EndpointsObjBuilder epb = new EndpointsObjBuilder()
                .setIgnore(false)
                .setProcessingRule(true);
        if (iPath.getSource().getIpv4Address() != null) {
            final Ipv4Builder ipBuilder = new Ipv4Builder()
                    .setDestinationIpv4Address(new Ipv4AddressNoZone(iPath.getDestination().getIpv4Address()))
                    .setSourceIpv4Address(new Ipv4AddressNoZone(iPath.getSource().getIpv4Address()));
            epb.setAddressFamily((new Ipv4CaseBuilder().setIpv4(ipBuilder.build()).build()));
        } else if (path.getIntendedPath().getSource().getIpv6Address() != null) {
            final Ipv6Builder ipBuilder = new Ipv6Builder()
                    .setDestinationIpv6Address(new Ipv6AddressNoZone(iPath.getDestination().getIpv6Address()))
                    .setSourceIpv6Address(new Ipv6AddressNoZone(iPath.getSource().getIpv6Address()));
            epb.setAddressFamily((new Ipv6CaseBuilder().setIpv6(ipBuilder.build()).build()));
        } else {
            // In case of ...
            return null;
        }

        // Create Path Setup Type
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

        // Create LSP
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        /*
         * Build Arguments.
         * Note that TE Metric and Delay are not set because, at least, Juniper Routers don't support them.
         */
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .setEndpointsObj(epb.build())
                .setEro(MessagesUtil.getEro(path.getActualPath().getPathDescription()))
                .addAugmentation(new Arguments2Builder()
                        .setLsp(lspBuilder.build())
                        .setPathSetupType(pstBuilder.build())
                        .build());

        // with Bandwidth and Standard Metric
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

        // Finally, build addLSP input
        return new AddLspInputBuilder()
                .setNode(teNode.getId())
                .setName(path.getName())
                .setArguments(args.build())
                .setNetworkTopologyRef(new NetworkTopologyRef(topologyRef))
                .build();
    }


    /**
     * Call add-lsp RPC to enforce the TE Path into the PCC. This action will trigger a PcInitiate message to the PCC.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Add LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<AddLspOutput>> addPath(final InstanceIdentifier<Topology> topologyRef,
            final NetworkTopologyPcepService ntps) {
        /* Check if we have a valid Path */
        if (path.getActualPath().getPathDescription() == null) {
            return null;
        }
        /* Check that is an Initiated Path */
        if (path.getPathType() != PathType.Initiated) {
            return null;
        }

        sent = true;
        final ListenableFuture<RpcResult<AddLspOutput>> enforce = ntps.addLsp(getAddLspInput(topologyRef));
        LOG.info("Call Add LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<AddLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<AddLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Enforce TE Path success {}", result.getResult());
                } else {
                    LOG.debug("Unable to enforce TE Path {} on Node {}: Got error {}", path.getName(), teNode.getId(),
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed enforce TE Path {} on Node {}", path.getName(), teNode.getId());
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    /**
     * Convert TE Path as Update LSP Input class to update it into the PCC through a call to update-lsp RPCs.
     *
     * @return  new Update LSP Input
     */
    private UpdateLspInput getUpdateLspInput(final InstanceIdentifier<Topology> topologyRef) {
        // Create Path Setup Type
        final IntendedPath iPath = path.getIntendedPath();
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

        // Create LSP
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        // Build Arguments
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder args;
        args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder()
                .addAugmentation(new Arguments3Builder()
                    .setLsp(lspBuilder.build())
                    .setPathSetupType(pstBuilder.build())
                    .build())
                .setEro(MessagesUtil.getEro(path.getActualPath().getPathDescription()));

        // with Bandwidth if defined, but not other Metrics as some routers don't support them
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

        // Finally, build updateLSP input
        return new UpdateLspInputBuilder()
                .setNode(teNode.getId())
                .setName(path.getName())
                .setArguments(args.build())
                .setNetworkTopologyRef(new NetworkTopologyRef(topologyRef))
                .build();
    }

    /**
     * Call update-lsp RPC to enforce the TE Path into the PCC. This action will trigger a PcUpdate message to the PCC.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Update LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<UpdateLspOutput>> updatePath(final InstanceIdentifier<Topology> topologyRef,
            final NetworkTopologyPcepService ntps) {

        /* Check if we have a valid ERO */
        if (path.getActualPath().getPathDescription() == null) {
            return null;
        }

        sent = true;
        final NodeId id = teNode.getId();
        final ListenableFuture<RpcResult<UpdateLspOutput>> enforce = ntps.updateLsp(getUpdateLspInput(topologyRef));
        LOG.info("Call Update LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<UpdateLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<UpdateLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Update TE Path success {}", result.getResult());
                } else {
                    LOG.debug("Unable to update TE Path {} on Node {}: Got error {}", path.getName(), id,
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed update TE Path {} on Node {}", path.getName(), id);
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    /**
     * Call remove-lsp RPC to remove the TE Path from the PCC. This action will trigger a PcInitiate message to the PCC
     * with 'R' bit set.
     *
     * @param ntps  Network Topology PCEP Service
     *
     * @return      Remove LSP Output to convey the RPC result
     */
    public ListenableFuture<RpcResult<RemoveLspOutput>> removePath(final InstanceIdentifier<Topology> topologyRef,
            final NetworkTopologyPcepService ntps) {

        /* Check if we could remove this path */
        if (path.getPathType() != PathType.Initiated) {
            return null;
        }

        sent = true;
        final NodeId id = teNode.getId();
        final RemoveLspInput rli = new RemoveLspInputBuilder()
                .setNode(id)
                .setName(path.getName())
                .setNetworkTopologyRef(new NetworkTopologyRef(topologyRef))
                .build();
        final ListenableFuture<RpcResult<RemoveLspOutput>> enforce = ntps.removeLsp(rli);
        LOG.info("Call Remove LSP to {} with {}", ntps, enforce);
        Futures.addCallback(enforce, new FutureCallback<RpcResult<RemoveLspOutput>>() {
            @Override
            public void onSuccess(final RpcResult<RemoveLspOutput> result) {
                if (result.isSuccessful()) {
                    LOG.debug("Delete TE Path success {}", result.getResult());
                } else {
                    LOG.debug("Unable to delete TE Path {} on Node {}: Got error {}", path.getName(), id,
                            result.getErrors());
                }
                sent = false;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failed delete TE Path {} on Node {}", path.getName(), id);
                sent = false;
            }
        }, MoreExecutors.directExecutor());

        return enforce;
    }

    private InstanceIdentifier<TePath> getTePathInstanceIdentifier(final NodeId id) {
        return MANAGED_PATH_IDENTIFIER.child(ManagedNode.class, new ManagedNodeKey(id)).child(TePath.class, path.key());
    }

    /**
     * Add TE Path components to the Data Store.
     */
    public synchronized void addToDataStore() {
        final WriteTransaction trans = teNode.getTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, getTePathInstanceIdentifier(teNode.getId()), path);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Managed TE Path {} has been published in operational datastore ", path.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot write Managed TE Path {} to the operational datastore (transaction: {})",
                        path.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Update TE Path components to the Data Store.
     */
    public synchronized void updateToDataStore() {
        final WriteTransaction trans = teNode.getTransaction();
        trans.merge(LogicalDatastoreType.OPERATIONAL, getTePathInstanceIdentifier(teNode.getId()), path);
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Managed TE Path {} has been updated in operational datastore ", path.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot update Managed TE Path {} to the operational datastore (transaction: {})",
                        path.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Remove TE Path components to the Data Store.
     */
    public synchronized void removeFromDataStore() {
        final WriteTransaction trans = teNode.getTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getTePathInstanceIdentifier(teNode.getId()));
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Managed TE Path {} has been deleted in operational datastore ", path.getName());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Cannot delete Managed TE Path {} from the operational datastore (transaction: {})",
                        path.getName(), trans.getIdentifier());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper("ManagedTePath");
        CodeHelpers.appendValue(helper, "TePath", path);
        return helper.toString();
    }
}
