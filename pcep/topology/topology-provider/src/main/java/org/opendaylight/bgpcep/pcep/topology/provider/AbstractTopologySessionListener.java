/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.concurrent.FutureListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.bgpcep.pcep.topology.provider.session.stats.SessionStateImpl;
import org.opendaylight.bgpcep.pcep.topology.provider.session.stats.TopologySessionStats;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.LspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.MessageHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.reported.lsp.PathKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for PCEP topology providers. It handles the common tasks involved in managing a PCEP server (PCE)
 * endpoint, and exposing a network topology based on it. It needs to be subclassed to form a fully functional block,
 * where the subclass provides handling of incoming messages.
 *
 * @param <S> identifier type of requests
 * @param <L> identifier type for LSPs
 */
public abstract class AbstractTopologySessionListener<S, L> implements TopologySessionListener, TopologySessionStats {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologySessionListener.class);

    static final String MISSING_XML_TAG = "Mandatory XML tags are missing.";
    static final MessageHeader MESSAGE_HEADER = new MessageHeader() {
        private final ProtocolVersion version = new ProtocolVersion(Uint8.ONE);

        @Override
        public Class<MessageHeader> implementedInterface() {
            return MessageHeader.class;
        }

        @Override
        public ProtocolVersion getVersion() {
            return version;
        }
    };

    @GuardedBy("this")
    final Map<L, String> lsps = new HashMap<>();
    @GuardedBy("this")
    SessionStateImpl listenerState;

    @GuardedBy("this")
    private final Map<S, PCEPRequest> requests = new HashMap<>();
    @GuardedBy("this")
    private final Map<String, ReportedLsp> lspData = new ConcurrentHashMap<>();
    private final ServerSessionManager serverSessionManager;
    private InstanceIdentifier<PathComputationClient> pccIdentifier;
    @GuardedBy("this")
    private TopologyNodeState nodeState;
    private final AtomicBoolean synced = new AtomicBoolean(false);
    @GuardedBy("this")
    private PCEPSession session;
    @GuardedBy("this")
    private SyncOptimization syncOptimization;
    @GuardedBy("this")
    private boolean triggeredResyncInProcess;

    AbstractTopologySessionListener(final ServerSessionManager serverSessionManager) {
        this.serverSessionManager = requireNonNull(serverSessionManager);
    }

    @Override
    public final void onSessionUp(final PCEPSession psession) {
        synchronized (this.serverSessionManager) {
            synchronized (this) {
                /*
                 * The session went up. Look up the router in Inventory model, create it if it
                 * is not there (marking that fact for later deletion), and mark it as
                 * synchronizing. Also create it in the topology model, with empty LSP list.
                 */
                final InetAddress peerAddress = psession.getRemoteAddress();

                this.syncOptimization = new SyncOptimization(psession);

                final TopologyNodeState state =
                        this.serverSessionManager.takeNodeState(peerAddress, this, isLspDbRetreived());

                // takeNodeState(..) may fail when the server session manager is being restarted
                // due to configuration change
                if (state == null) {
                    LOG.error("Unable to fetch topology node state for PCEP session. Closing session {}", psession);
                    psession.close(TerminationReason.UNKNOWN);
                    this.onSessionTerminated(psession, new PCEPCloseTermination(TerminationReason.UNKNOWN));
                    return;
                }

                if (this.session != null || this.nodeState != null) {
                    LOG.error("PCEP session is already up with {}. Closing session {}", psession.getRemoteAddress(),
                            psession);
                    psession.close(TerminationReason.UNKNOWN);
                    this.onSessionTerminated(psession, new PCEPCloseTermination(TerminationReason.UNKNOWN));
                    return;
                }
                this.session = psession;
                this.nodeState = state;

                LOG.trace("Peer {} resolved to topology node {}", peerAddress, state.getNodeId());

                // Our augmentation in the topology node
                final PathComputationClientBuilder pccBuilder = new PathComputationClientBuilder();

                onSessionUp(psession, pccBuilder);
                this.synced.set(isSynchronized());

                pccBuilder.setIpAddress(IetfInetUtil.INSTANCE.ipAddressNoZoneFor(peerAddress));
                final InstanceIdentifier<Node1> topologyAugment = state.getNodeId().augmentation(Node1.class);
                this.pccIdentifier = topologyAugment.child(PathComputationClient.class);
                final Node initialNodeState = state.getInitialNodeState();
                final boolean isNodePresent = isLspDbRetreived() && initialNodeState != null;
                if (isNodePresent) {
                    loadLspData(initialNodeState, this.lspData, this.lsps, isIncrementalSynchro());
                    pccBuilder.setReportedLsp(
                            initialNodeState.augmentation(Node1.class).getPathComputationClient().getReportedLsp());
                }
                state.storeNode(topologyAugment,
                        new Node1Builder().setPathComputationClient(pccBuilder.build()).build(), psession);

                this.listenerState = new SessionStateImpl(this, psession);
                this.serverSessionManager.bind(state.getNodeId(), this.listenerState);
                LOG.info("Session with {} attached to topology node {}", psession.getRemoteAddress(),
                        state.getNodeId());
            }
        }
    }

    protected abstract void onSessionUp(PCEPSession session, PathComputationClientBuilder pccBuilder);

    synchronized void updatePccState(final PccSyncState pccSyncState) {
        if (this.nodeState == null) {
            LOG.info("Server Session Manager is closed.");
            AbstractTopologySessionListener.this.session.close(TerminationReason.UNKNOWN);
            return;
        }
        final MessageContext ctx = new MessageContext(this.nodeState.getChain().newWriteOnlyTransaction());
        updatePccNode(ctx, new PathComputationClientBuilder().setStateSync(pccSyncState).build());
        if (pccSyncState != PccSyncState.Synchronized) {
            this.synced.set(false);
            this.triggeredResyncInProcess = true;
        }
        // All set, commit the modifications
        ctx.trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Pcc Internal state for session {} updated successfully",
                        AbstractTopologySessionListener.this.session);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to update Pcc internal state for session {}",
                        AbstractTopologySessionListener.this.session, throwable);
                AbstractTopologySessionListener.this.session.close(TerminationReason.UNKNOWN);
            }
        }, MoreExecutors.directExecutor());
    }

    synchronized boolean isTriggeredSyncInProcess() {
        return this.triggeredResyncInProcess;
    }

    /**
     * Tear down the given PCEP session. It's OK to call this method even after the session
     * is already down. It always clear up the current session status.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void tearDown(final PCEPSession psession) {
        requireNonNull(psession);
        synchronized (this.serverSessionManager) {
            synchronized (this) {
                this.serverSessionManager.releaseNodeState(this.nodeState, psession, isLspDbPersisted());
                clearNodeState();

                try {
                    if (this.session != null) {
                        this.session.close();
                    }
                    psession.close();
                } catch (final Exception e) {
                    LOG.error("Session {} cannot be closed.", psession, e);
                }
                this.session = null;
                this.listenerState = null;
                this.syncOptimization = null;

                // Clear all requests we know about
                for (final Entry<S, PCEPRequest> e : this.requests.entrySet()) {
                    final PCEPRequest r = e.getValue();
                    switch (r.getState()) {
                        case DONE:
                            // Done is done, nothing to do
                            LOG.trace("Request {} was done when session went down.", e.getKey());
                            break;
                        case UNACKED:
                            // Peer has not acked: results in failure
                            LOG.info("Request {} was incomplete when session went down, failing the instruction",
                                    e.getKey());
                            r.done(OperationResults.NOACK);
                            break;
                        case UNSENT:
                            // Peer has not been sent to the peer: results in cancellation
                            LOG.debug("Request {} was not sent when session went down, cancelling the instruction",
                                    e.getKey());
                            r.done(OperationResults.UNSENT);
                            break;
                        default:
                            break;
                    }
                }
                this.requests.clear();
            }
        }
    }

    @Override
    public final void onSessionDown(final PCEPSession psession, final Exception exception) {
        LOG.warn("Session {} went down unexpectedly", psession, exception);
        tearDown(psession);
    }

    @Override
    public final void onSessionTerminated(final PCEPSession psession, final PCEPTerminationReason reason) {
        LOG.info("Session {} terminated by peer with reason {}", psession, reason);
        tearDown(psession);
    }

    @Override
    public final synchronized void onMessage(final PCEPSession psession, final Message message) {
        if (this.nodeState == null) {
            LOG.warn("Topology node state is null. Unhandled message {} on session {}", message, psession);
            psession.close(TerminationReason.UNKNOWN);
            return;
        }
        final MessageContext ctx = new MessageContext(this.nodeState.getChain().newWriteOnlyTransaction());

        if (onMessage(ctx, message)) {
            LOG.warn("Unhandled message {} on session {}", message, psession);
            //cancel not supported, submit empty transaction
            ctx.trans.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.trace("Successful commit");
                }

                @Override
                public void onFailure(final Throwable trw) {
                    LOG.error("Failed commit", trw);
                }
            }, MoreExecutors.directExecutor());
            return;
        }

        ctx.trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Internal state for session {} updated successfully", psession);
                ctx.notifyRequests();

            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to update internal state for session {}, closing it", psession, throwable);
                ctx.notifyRequests();
                psession.close(TerminationReason.UNKNOWN);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Perform revision-specific message processing when a message arrives.
     *
     * @param ctx     Message processing context
     * @param message Protocol message
     * @return True if the message type is not handle.
     */
    protected abstract boolean onMessage(MessageContext ctx, Message message);

    @Override
    public void close() {
        synchronized (this.serverSessionManager) {
            synchronized (this) {
                clearNodeState();
                if (this.session != null) {
                    LOG.info("Closing session {}", session);
                    this.session.close(TerminationReason.UNKNOWN);
                }
            }
        }
    }

    @Holding({"this.serverSessionManager", "this"})
    private void clearNodeState() {
        if (this.nodeState != null) {
            this.serverSessionManager.unbind(this.nodeState.getNodeId());
            LOG.debug("Clear Node state: {}", this.nodeState.getNodeId());
            this.nodeState = null;
        }
    }

    final synchronized PCEPRequest removeRequest(final S id) {
        final PCEPRequest ret = this.requests.remove(id);
        if (ret != null && this.listenerState != null) {
            this.listenerState.processRequestStats(ret.getElapsedMillis());
        }
        LOG.trace("Removed request {} object {}", id, ret);
        return ret;
    }

    final synchronized ListenableFuture<OperationResult> sendMessage(final Message message, final S requestId,
            final Metadata metadata) {
        final var sendFuture = this.session.sendMessage(message);
        this.listenerState.updateStatefulSentMsg(message);
        final PCEPRequest req = new PCEPRequest(metadata);
        this.requests.put(requestId, req);
        final short rpcTimeout = this.serverSessionManager.getRpcTimeout();
        LOG.trace("RPC response timeout value is {} seconds", rpcTimeout);
        if (rpcTimeout > 0) {
            setupTimeoutHandler(requestId, req, rpcTimeout);
        }

        sendFuture.addListener((FutureListener<Void>) future -> {
            if (!future.isSuccess()) {
                synchronized (AbstractTopologySessionListener.this) {
                    AbstractTopologySessionListener.this.requests.remove(requestId);
                }
                req.done(OperationResults.UNSENT);
                LOG.info("Failed to send request {}, instruction cancelled", requestId, future.cause());
            } else {
                req.sent();
                LOG.trace("Request {} sent to peer (object {})", requestId, req);
            }
        });

        return req.getFuture();
    }

    private void setupTimeoutHandler(final S requestId, final PCEPRequest req, final short timeout) {
        final Timer timer = req.getTimer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (AbstractTopologySessionListener.this) {
                    AbstractTopologySessionListener.this.requests.remove(requestId);
                }
                req.done();
                LOG.info("Request {} timed-out waiting for response", requestId);
            }
        }, TimeUnit.SECONDS.toMillis(timeout));
        LOG.trace("Set up response timeout handler for request {}", requestId);
    }

    /**
     * Update an LSP in the data store.
     *
     * @param ctx       Message context
     * @param id        Revision-specific LSP identifier
     * @param lspName   LSP name
     * @param rlb       Reported LSP builder
     * @param solicited True if the update was solicited
     * @param remove    True if this is an LSP path removal
     */
    protected final synchronized void updateLsp(final MessageContext ctx, final L id, final String lspName,
            final ReportedLspBuilder rlb, final boolean solicited, final boolean remove) {

        final String name;
        if (lspName == null) {
            name = this.lsps.get(id);
            if (name == null) {
                LOG.error("PLSPID {} seen for the first time, not reporting the LSP", id);
                return;
            }
        } else {
            name = lspName;
        }

        LOG.debug("Saved LSP {} with name {}", id, name);
        this.lsps.put(id, name);


        final ReportedLsp previous = this.lspData.get(name);
        // if no previous report about the lsp exist, just proceed
        if (previous != null) {
            final Map<PathKey, Path> updatedPaths = makeBeforeBreak(rlb, previous, name, remove);
            // if all paths or the last path were deleted, delete whole tunnel
            if (updatedPaths.isEmpty()) {
                LOG.debug("All paths were removed, removing LSP with {}.", id);
                removeLsp(ctx, id);
                return;
            }
            rlb.setPath(updatedPaths);
        }
        rlb.withKey(new ReportedLspKey(name));
        rlb.setName(name);

        // If this is an unsolicited update. We need to make sure we retain the metadata already present
        if (solicited) {
            this.nodeState.setLspMetadata(name, rlb.getMetadata());
        } else {
            rlb.setMetadata(this.nodeState.getLspMetadata(name));
        }

        final ReportedLsp rl = rlb.build();
        ctx.trans.put(LogicalDatastoreType.OPERATIONAL, this.pccIdentifier.child(ReportedLsp.class, rlb.key()), rl);
        LOG.debug("LSP {} updated to MD-SAL", name);

        this.lspData.put(name, rl);
    }

    private static Map<PathKey, Path> makeBeforeBreak(final ReportedLspBuilder rlb, final ReportedLsp previous,
            final String name, final boolean remove) {
        // just one path should be reported
        final Path path = Iterables.getOnlyElement(rlb.getPath().values());
        final var reportedLspId = path.getLspId();
        final List<Path> updatedPaths;
        //lspId = 0 and remove = false -> tunnel is down, still exists but no path is signaled
        //remove existing tunnel's paths now, as explicit path remove will not come
        if (!remove && reportedLspId.getValue().toJava() == 0) {
            updatedPaths = new ArrayList<>();
            LOG.debug("Remove previous paths {} to this lsp name {}", previous.getPath(), name);
        } else {
            // check previous report for existing paths
            final Collection<Path> prev = previous.nonnullPath().values();
            updatedPaths = new ArrayList<>(prev);
            LOG.debug("Found previous paths {} to this lsp name {}", updatedPaths, name);
            for (final Path prevPath : prev) {
                //we found reported path in previous reports
                if (prevPath.getLspId().getValue().toJava() == 0 || prevPath.getLspId().equals(reportedLspId)) {
                    LOG.debug("Match on lsp-id {}", prevPath.getLspId().getValue());
                    // path that was reported previously and does have the same lsp-id, path will be updated
                    final boolean r = updatedPaths.remove(prevPath);
                    LOG.trace("Request removed? {}", r);
                }
            }
        }
        // if the path does not exist in previous report, add it to path list, it's a new ERO
        // only one path will be added
        //lspId is 0 means confirmation message that shouldn't be added (because we have no means of deleting it later)
        LOG.trace("Adding new path {} to {}", path, updatedPaths);
        updatedPaths.add(path);
        if (remove) {
            if (reportedLspId.getValue().toJava() == 0) {
                // if lsp-id also 0, remove all paths
                LOG.debug("Removing all paths.");
                updatedPaths.clear();
            } else {
                // path is marked to be removed
                LOG.debug("Removing path {} from {}", path, updatedPaths);
                final boolean r = updatedPaths.remove(path);
                LOG.trace("Request removed? {}", r);
            }
        }
        LOG.debug("Setting new paths {} to lsp {}", updatedPaths, name);
        return Maps.uniqueIndex(updatedPaths, Path::key);
    }

    /**
     * Indicate that the peer has completed state synchronization.
     *
     * @param ctx Message context
     */
    protected final synchronized void stateSynchronizationAchieved(final MessageContext ctx) {
        if (this.synced.getAndSet(true)) {
            LOG.debug("State synchronization achieved while synchronizing, not updating state");
            return;
        }
        if (this.triggeredResyncInProcess) {
            this.triggeredResyncInProcess = false;
        }
        updatePccNode(ctx, new PathComputationClientBuilder().setStateSync(PccSyncState.Synchronized).build());

        // The node has completed synchronization, cleanup metadata no longer reported back
        this.nodeState.cleanupExcept(this.lsps.values());
        LOG.debug("Session {} achieved synchronized state", this.session);
    }

    protected final synchronized void updatePccNode(final MessageContext ctx, final PathComputationClient pcc) {
        ctx.trans.merge(LogicalDatastoreType.OPERATIONAL, this.pccIdentifier, pcc);
    }

    protected final InstanceIdentifier<ReportedLsp> lspIdentifier(final String name) {
        return this.pccIdentifier.child(ReportedLsp.class, new ReportedLspKey(name));
    }

    /**
     * Remove LSP from the database.
     *
     * @param ctx Message Context
     * @param id  Revision-specific LSP identifier
     */
    protected final synchronized void removeLsp(final MessageContext ctx, final L id) {
        final String name = this.lsps.remove(id);
        LOG.debug("LSP {} removed", name);
        ctx.trans.delete(LogicalDatastoreType.OPERATIONAL, lspIdentifier(name));
        this.lspData.remove(name);
    }

    @Holding("this")
    final String lookupLspName(final L id) {
        return this.lsps.get(requireNonNull(id, "ID parameter null."));
    }

    /**
     * Reads operational data on this node. Doesn't attempt to read the data,
     * if the node does not exist. In this case returns null.
     *
     * @param id InstanceIdentifier of the node
     * @return null if the node does not exists, or operational data
     */
    final synchronized <T extends DataObject> FluentFuture<Optional<T>>
        readOperationalData(final InstanceIdentifier<T> id) {
        if (this.nodeState == null) {
            return null;
        }
        return this.nodeState.readOperationalData(id);
    }

    protected abstract Object validateReportedLsp(Optional<ReportedLsp> rep, LspId input);

    protected abstract void loadLspData(Node node, Map<String, ReportedLsp> lspData, Map<L, String> lsps,
            boolean incrementalSynchro);

    final boolean isLspDbPersisted() {
        return this.syncOptimization != null && this.syncOptimization.isSyncAvoidanceEnabled();
    }

    final boolean isLspDbRetreived() {
        return this.syncOptimization != null && this.syncOptimization.isDbVersionPresent();
    }

    /**
     * Is Incremental synchronization if LSP-DB-VERSION are included,
     * LSP-DB-VERSION TLV values doesnt match, and  LSP-SYNC-CAPABILITY is enabled.
     */
    final synchronized boolean isIncrementalSynchro() {
        return this.syncOptimization != null && this.syncOptimization.isSyncAvoidanceEnabled()
                && this.syncOptimization.isDeltaSyncEnabled();
    }

    final synchronized boolean isTriggeredInitialSynchro() {
        return this.syncOptimization != null && this.syncOptimization.isTriggeredInitSyncEnabled();
    }

    final synchronized boolean isTriggeredReSyncEnabled() {
        return this.syncOptimization != null && this.syncOptimization.isTriggeredReSyncEnabled();
    }

    protected final synchronized boolean isSynchronized() {
        return this.syncOptimization != null && this.syncOptimization.doesLspDbMatch();
    }

    @Override
    public int getDelegatedLspsCount() {
        return Math.toIntExact(this.lspData.values().stream()
            .map(ReportedLsp::getPath).filter(pathList -> pathList != null && !pathList.isEmpty())
            // pick the first path, as delegate status should be same in each path
            .map(pathList -> pathList.values().iterator().next())
            .map(path -> path.augmentation(Path1.class)).filter(Objects::nonNull)
            .map(LspObject::getLsp).filter(Objects::nonNull)
            .filter(Lsp::getDelegate)
            .count());
    }

    @Override
    public boolean isSessionSynchronized() {
        return this.synced.get();
    }

    @Override
    public synchronized ListenableFuture<RpcResult<Void>> tearDownSession(final TearDownSessionInput input) {
        close();
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    static final class MessageContext {
        private final Collection<PCEPRequest> requests = new ArrayList<>();
        private final WriteTransaction trans;

        private MessageContext(final WriteTransaction trans) {
            this.trans = requireNonNull(trans);
        }

        void resolveRequest(final PCEPRequest req) {
            requests.add(req);
        }

        @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
                justification = "https://github.com/spotbugs/spotbugs/issues/811")
        private void notifyRequests() {
            for (final PCEPRequest r : requests) {
                r.done(OperationResults.SUCCESS);
            }
        }
    }
}
