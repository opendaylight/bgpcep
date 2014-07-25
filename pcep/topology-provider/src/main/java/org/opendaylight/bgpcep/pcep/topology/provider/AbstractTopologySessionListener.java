/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.netty.util.concurrent.FutureListener;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MessageHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
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
public abstract class AbstractTopologySessionListener<S, L> implements PCEPSessionListener, TopologySessionListener {
    protected static final class MessageContext {
        private final Collection<PCEPRequest> requests = new ArrayList<>();
        private final WriteTransaction trans;

        private MessageContext(final WriteTransaction trans) {
            this.trans = Preconditions.checkNotNull(trans);
        }

        void resolveRequest(final PCEPRequest req) {
            requests.add(req);
        }

        private void notifyRequests() {
            for (PCEPRequest r : requests) {
                r.done(OperationResults.SUCCESS);
            }
        }
    }

    protected static final MessageHeader MESSAGE_HEADER = new MessageHeader() {
        private final ProtocolVersion version = new ProtocolVersion((short) 1);

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MessageHeader.class;
        }

        @Override
        public ProtocolVersion getVersion() {
            return this.version;
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologySessionListener.class);

    private final Map<S, PCEPRequest> requests = new HashMap<>();
    private final Map<String, ReportedLsp> lspData = new HashMap<>();
    private final Map<L, String> lsps = new HashMap<>();
    private final ServerSessionManager serverSessionManager;
    private InstanceIdentifier<Node> topologyNode;
    private InstanceIdentifier<Node1> topologyAugment;
    private InstanceIdentifier<PathComputationClient> pccIdentifier;
    private TopologyNodeState nodeState;
    private boolean ownsTopology = false;
    private boolean synced = false;
    private PCEPSession session;

    protected AbstractTopologySessionListener(final ServerSessionManager serverSessionManager) {
        this.serverSessionManager = Preconditions.checkNotNull(serverSessionManager);
    }

    private static String createNodeId(final InetAddress addr) {
        return "pcc://" + addr.getHostAddress();
    }

    private Node topologyNode(final ReadWriteTransaction trans, final InetAddress address) {
        final String pccId = createNodeId(address);

        // FIXME: Futures.transform...
        try {
            Optional<Topology> topoMaybe = trans.read(LogicalDatastoreType.OPERATIONAL, this.serverSessionManager.getTopology()).get();
            Preconditions.checkState(topoMaybe.isPresent(), "Failed to find topology.");
            final Topology topo = topoMaybe.get();
            for (final Node n : topo.getNode()) {
                LOG.debug("Matching topology node {} to id {}", n, pccId);
                if (n.getNodeId().getValue().equals(pccId)) {
                    this.topologyNode = this.serverSessionManager.getTopology().child(Node.class, n.getKey());
                    LOG.debug("Reusing topology node {} for id {} at {}", n, pccId, this.topologyNode);
                    return n;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to ensure topology presence.", e);
        }

        /*
         * We failed to find a matching node. Let's create a dynamic one
         * and note that we are the owner (so we clean it up afterwards).
         */
        final NodeId id = new NodeId(pccId);
        final NodeKey nk = new NodeKey(id);
        final InstanceIdentifier<Node> nti = this.serverSessionManager.getTopology().child(Node.class, nk);

        final Node ret = new NodeBuilder().setKey(nk).setNodeId(id).build();

        trans.put(LogicalDatastoreType.OPERATIONAL, nti, ret);
        LOG.debug("Created topology node {} for id {} at {}", ret, pccId, nti);
        this.ownsTopology = true;
        this.topologyNode = nti;
        return ret;
    }

    @Override
    public final synchronized void onSessionUp(final PCEPSession session) {
        /*
         * The session went up. Look up the router in Inventory model,
         * create it if it is not there (marking that fact for later
         * deletion), and mark it as synchronizing. Also create it in
         * the topology model, with empty LSP list.
         */
        final InetAddress peerAddress = session.getRemoteAddress();
        final ReadWriteTransaction trans = this.serverSessionManager.rwTransaction();

        final Node topoNode = topologyNode(trans, peerAddress);
        LOG.trace("Peer {} resolved to topology node {}", peerAddress, topoNode);

        // Our augmentation in the topology node
        this.synced = false;

        final PathComputationClientBuilder pccBuilder;
        pccBuilder = new PathComputationClientBuilder();
        pccBuilder.setIpAddress(IpAddressBuilder.getDefaultInstance(peerAddress.getHostAddress()));

        onSessionUp(session, pccBuilder);

        final Node1 ta = new Node1Builder().setPathComputationClient(pccBuilder.build()).build();

        this.topologyAugment = this.topologyNode.augmentation(Node1.class);
        this.pccIdentifier = topologyAugment.child(PathComputationClient.class);

        trans.put(LogicalDatastoreType.OPERATIONAL, this.topologyAugment, ta);
        LOG.trace("Peer data {} set to {}", this.topologyAugment, ta);

        // All set, commit the modifications
        Futures.addCallback(trans.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Internal state for session {} updated successfully", session);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to update internal state for session {}, terminating it", session, t);
                session.close(TerminationReason.Unknown);
            }
        });

        this.nodeState = this.serverSessionManager.takeNodeState(topoNode.getNodeId(), this);
        this.session = session;
        LOG.info("Session with {} attached to topology node {}", session.getRemoteAddress(), topoNode.getNodeId());
    }

    @GuardedBy("this")
    private void tearDown(final PCEPSession session) {
        this.serverSessionManager.releaseNodeState(this.nodeState);
        this.nodeState = null;
        this.session = null;

        // The session went down. Undo all the Topology changes we have done.
        final WriteTransaction trans = this.serverSessionManager.beginTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, this.topologyAugment);
        if (this.ownsTopology) {
            trans.delete(LogicalDatastoreType.OPERATIONAL, this.topologyNode);
        }

        Futures.addCallback(trans.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Internal state for session {} cleaned up successfully", session);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to cleanup internal state for session {}", session, t);
            }
        });

        // Clear all requests we know about
        for (final Entry<S, PCEPRequest> e : this.requests.entrySet()) {
            final PCEPRequest r = e.getValue();
            switch (r.getState()) {
            case DONE:
                // Done is done, nothing to do
                break;
            case UNACKED:
                // Peer has not acked: results in failure
                LOG.info("Request {} was incomplete when session went down, failing the instruction", e.getKey());
                r.done(OperationResults.NOACK);
                break;
            case UNSENT:
                // Peer has not been sent to the peer: results in cancellation
                LOG.debug("Request {} was not sent when session went down, cancelling the instruction", e.getKey());
                r.done(OperationResults.UNSENT);
                break;
            }
        }
        this.requests.clear();
    }

    @Override
    public final synchronized void onSessionDown(final PCEPSession session, final Exception e) {
        LOG.warn("Session {} went down unexpectedly", session, e);
        tearDown(session);
    }

    @Override
    public final synchronized void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason reason) {
        LOG.info("Session {} terminated by peer with reason {}", session, reason);
        tearDown(session);
    }

    @Override
    public final synchronized void onMessage(final PCEPSession session, final Message message) {
        final MessageContext ctx = new MessageContext(this.serverSessionManager.beginTransaction());

        if (onMessage(ctx, message)) {
            LOG.info("Unhandled message {} on session {}", message, session);
            return;
        }

        Futures.addCallback(ctx.trans.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Internal state for session {} updated successfully", session);
                ctx.notifyRequests();
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to update internal state for session {}, closing it", session, t);
                ctx.notifyRequests();
                session.close(TerminationReason.Unknown);
            }
        });
    }

    @Override
    public void close() {
        if (this.session != null) {
            this.session.close(TerminationReason.Unknown);
        }
    }

    protected final synchronized PCEPRequest removeRequest(final S id) {
        final PCEPRequest ret = this.requests.remove(id);
        LOG.trace("Removed request {} object {}", id, ret);
        return ret;
    }

    protected final synchronized ListenableFuture<OperationResult> sendMessage(final Message message, final S requestId,
            final Metadata metadata) {
        final io.netty.util.concurrent.Future<Void> f = this.session.sendMessage(message);
        final PCEPRequest req = new PCEPRequest(metadata);
        this.requests.put(requestId, req);

        f.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(final io.netty.util.concurrent.Future<Void> future) {
                if (!future.isSuccess()) {
                    synchronized (AbstractTopologySessionListener.this) {
                        requests.remove(requestId);
                    }
                    req.done(OperationResults.UNSENT);
                    LOG.info("Failed to send request {}, instruction cancelled", requestId, future.cause());
                } else {
                    req.sent();
                    LOG.trace("Request {} sent to peer (object {})", requestId, req);
                }
            }
        });

        return req.getFuture();
    }

    /**
     * Update an LSP in the data store
     *
     * @param ctx Message context
     * @param id Revision-specific LSP identifier
     * @param lspName LSP name
     * @param rlb Reported LSP builder
     * @param solicited True if the update was solicited
     * @param remove True if this is an LSP path removal
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

        // just one path should be reported
        Preconditions.checkState(rlb.getPath().size() == 1);
        LspId reportedLspId = rlb.getPath().get(0).getLspId();
        // check previous report for existing paths
        ReportedLsp previous = this.lspData.get(name);
        // if no previous report about the lsp exist, just proceed
        if (previous != null) {
            List<Path> updatedPaths = new ArrayList<>(previous.getPath());
            LOG.debug("Found previous paths {} to this lsp name {}", updatedPaths, name);
            for (Path path : previous.getPath()) {
                //we found reported path in previous reports
                if (path.getLspId().getValue() == 0 || path.getLspId().equals(reportedLspId)) {
                    LOG.debug("Match on lsp-id {}", path.getLspId().getValue() );
                    // path that was reported previously and does have the same lsp-id, path will be updated
                    final boolean r = updatedPaths.remove(path);
                    LOG.trace("Request removed? {}", r);
                }
            }
            // if the path does not exist in previous report, add it to path list, it's a new ERO
            // only one path will be added
            //lspId is 0 means confirmation message that shouldn't be added (because we have no means of deleting it later)
            LOG.trace("Adding new path {} to {}", rlb.getPath(), updatedPaths);
            updatedPaths.addAll(rlb.getPath());
            if (remove) {
                if (reportedLspId.getValue() == 0) {
                    // if lsp-id also 0, remove all paths
                    LOG.debug("Removing all paths.");
                    updatedPaths.clear();
                } else {
                    // path is marked to be removed
                    LOG.debug("Removing path {} from {}", rlb.getPath(), updatedPaths);
                    final boolean r = updatedPaths.removeAll(rlb.getPath());
                    LOG.trace("Request removed? {}", r);
                }
            }
            // if all paths or the last path were deleted, delete whole tunnel
            if (updatedPaths.isEmpty()) {
                LOG.debug("All paths were removed, removing LSP with {}.", id);
                removeLsp(ctx, id);
                return;
            }
            LOG.debug("Setting new paths {} to lsp {}", updatedPaths, name);
            rlb.setPath(updatedPaths);
        }
        Preconditions.checkState(name != null);
        rlb.setKey(new ReportedLspKey(name));
        rlb.setName(name);

        // If this is an unsolicited update. We need to make sure we retain the metadata already present
        if (solicited) {
            this.nodeState.setLspMetadata(name, rlb.getMetadata());
        } else {
            rlb.setMetadata(this.nodeState.getLspMetadata(name));
        }

        final ReportedLsp rl = rlb.build();
        ctx.trans.put(LogicalDatastoreType.OPERATIONAL, this.pccIdentifier.child(ReportedLsp.class, rlb.getKey()), rl);
        LOG.debug("LSP {} updated to MD-SAL", name);

        this.lspData.put(name, rl);
    }

    /**
     * Indicate that the peer has completed state synchronization.
     *
     * @param ctx Message context
     */
    protected final synchronized void stateSynchronizationAchieved(final MessageContext ctx) {
        if (this.synced) {
            LOG.debug("State synchronization achieved while synchronized, not updating state");
            return;
        }

        // Update synchronization flag
        this.synced = true;
        ctx.trans.merge(LogicalDatastoreType.OPERATIONAL, this.pccIdentifier, new PathComputationClientBuilder().setStateSync(PccSyncState.Synchronized).build());

        // The node has completed synchronization, cleanup metadata no longer reported back
        this.nodeState.cleanupExcept(this.lsps.values());
        LOG.debug("Session {} achieved synchronized state", this.session);
    }

    protected final InstanceIdentifier<ReportedLsp> lspIdentifier(final String name) {
        return pccIdentifier.child(ReportedLsp.class, new ReportedLspKey(name));
    }

    /**
     * Remove LSP from the database.
     *
     * @param ctx Message Context
     * @param id Revision-specific LSP identifier
     */
    protected final synchronized void removeLsp(final MessageContext ctx, final L id) {
        final String name = this.lsps.remove(id);
        LOG.debug("LSP {} removed", name);
        ctx.trans.delete(LogicalDatastoreType.OPERATIONAL, lspIdentifier(name));
        this.lspData.remove(name);
    }

    protected abstract void onSessionUp(PCEPSession session, PathComputationClientBuilder pccBuilder);

    /**
     * Perform revision-specific message processing when a message arrives.
     *
     * @param ctx Message processing context
     * @param message Protocol message
     * @return True if the message type is not handle.
     */
    protected abstract boolean onMessage(MessageContext ctx, Message message);

    protected final String lookupLspName(final L id) {
        Preconditions.checkNotNull(id, "ID parameter null.");
        return this.lsps.get(id);
    }

    protected final <T extends DataObject> ListenableFuture<Optional<T>> readOperationalData(final InstanceIdentifier<T> id) {
        return this.serverSessionManager.readOperationalData(id);
    }
}
