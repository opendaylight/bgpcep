/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.util.concurrent.FutureListener;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MessageHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractTopologySessionListener<SRPID, PLSPID, PATHNAME> implements PCEPSessionListener, TopologySessionListener {
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
	protected static final Pcerr UNHANDLED_MESSAGE_ERROR = new PcerrBuilder().setPcerrMessage(
			new PcerrMessageBuilder().setErrorType(null).build()).build();

	private static final Logger LOG = LoggerFactory.getLogger(Stateful07TopologySessionListener.class);

	// FIXME: make this private
	protected final ServerSessionManager serverSessionManager;

	private final Map<SRPID, PCEPRequest> waitingRequests = new HashMap<>();
	private final Map<SRPID, PCEPRequest> sendingRequests = new HashMap<>();
	private final Map<PLSPID, PATHNAME> lsps = new HashMap<>();
	private InstanceIdentifier<Node> topologyNode;
	private InstanceIdentifier<Node1> topologyAugment;
	private PathComputationClientBuilder pccBuilder;
	private Node1Builder topologyAugmentBuilder;
	private TopologyNodeState<PATHNAME> nodeState;
	private boolean ownsTopology = false;
	private boolean synced = false;
	private PCEPSession session;

	protected AbstractTopologySessionListener(final ServerSessionManager serverSessionManager) {
		this.serverSessionManager = Preconditions.checkNotNull(serverSessionManager);
	}

	private static String createNodeId(final InetAddress addr) {
		return "pcc://" + addr.getHostAddress();
	}

	private Node topologyNode(final DataModificationTransaction trans, final InetAddress address) {
		final String pccId = createNodeId(address);
		final Topology topo = (Topology) trans.readOperationalData(this.serverSessionManager.getTopology());

		for (final Node n : topo.getNode()) {
			LOG.debug("Matching topology node {} to id {}", n, pccId);
			if (n.getNodeId().getValue().equals(pccId)) {
				this.topologyNode =
						InstanceIdentifier.builder(this.serverSessionManager.getTopology()).child(Node.class, n.getKey()).toInstance();
				LOG.debug("Reusing topology node {} for id {} at {}", n, pccId, this.topologyNode);
				return n;
			}
		}

		/*
		 * We failed to find a matching node. Let's create a dynamic one
		 * and note that we are the owner (so we clean it up afterwards).
		 */
		final NodeId id = new NodeId(pccId);
		final NodeKey nk = new NodeKey(id);
		final InstanceIdentifier<Node> nti = InstanceIdentifier.builder(this.serverSessionManager.getTopology()).child(Node.class, nk).toInstance();

		final Node ret = new NodeBuilder().setKey(nk).setNodeId(id).build();

		trans.putOperationalData(nti, ret);
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
		final DataModificationTransaction trans = this.serverSessionManager.beginTransaction();

		final Node topoNode = topologyNode(trans, peerAddress);
		LOG.debug("Peer {} resolved to topology node {}", peerAddress, topoNode);

		// Our augmentation in the topology node
		this.synced = false;
		this.pccBuilder = new PathComputationClientBuilder();
		this.pccBuilder.setIpAddress(IpAddressBuilder.getDefaultInstance(peerAddress.getHostAddress()));

		onSessionUp(session, this.pccBuilder);

		this.topologyAugmentBuilder = new Node1Builder().setPathComputationClient(this.pccBuilder.build());
		this.topologyAugment = InstanceIdentifier.builder(this.topologyNode).augmentation(Node1.class).toInstance();
		final Node1 ta = topologyAugmentBuilder.build();

		trans.putOperationalData(this.topologyAugment, ta);
		LOG.debug("Peer data {} set to {}", this.topologyAugment, ta);

		// All set, commit the modifications
		final ListenableFuture<RpcResult<TransactionStatus>> f = JdkFutureAdapters.listenInPoolThread(trans.commit());
		Futures.addCallback(f, new FutureCallback<RpcResult<TransactionStatus>>() {
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

		final DataModificationTransaction trans = this.serverSessionManager.beginTransaction();

		// The session went down. Undo all the Topology changes we have done.
		trans.removeOperationalData(this.topologyAugment);
		if (this.ownsTopology) {
			trans.removeOperationalData(this.topologyNode);
		}

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				LOG.trace("Internal state for session {} cleaned up successfully", session);
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to cleanup internal state for session {}", session, t);
			}
		});

		// Clear all requests which have not been sent to the peer: they result in cancellation
		for (final Entry<SRPID, PCEPRequest> e : this.sendingRequests.entrySet()) {
			LOG.debug("Request {} was not sent when session went down, cancelling the instruction", e.getKey());
			e.getValue().setResult(OperationResults.UNSENT);
		}
		this.sendingRequests.clear();

		// CLear all requests which have not been acked by the peer: they result in failure
		for (final Entry<SRPID, PCEPRequest> e : this.waitingRequests.entrySet()) {
			LOG.info("Request {} was incomplete when session went down, failing the instruction", e.getKey());
			e.getValue().setResult(OperationResults.NOACK);
		}
		this.waitingRequests.clear();
	}

	@Override
	public final synchronized void onSessionDown(final PCEPSession session, final Exception e) {
		LOG.warn("Session {} went down unexpectedly", e);
		tearDown(session);
	}

	@Override
	public final synchronized void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason reason) {
		LOG.info("Session {} terminated by peer with reason {}", session, reason);
		tearDown(session);
	}

	protected InstanceIdentifierBuilder<PathComputationClient> pccIdentifier() {
		return InstanceIdentifier.builder(this.topologyAugment).child(PathComputationClient.class);
	}

	protected final synchronized PCEPRequest removeRequest(final SRPID id) {
		return this.waitingRequests.remove(id);
	}

	private synchronized void messageSendingComplete(final SRPID requestId, final io.netty.util.concurrent.Future<Void> future) {
		final PCEPRequest req = this.sendingRequests.remove(requestId);

		if (future.isSuccess()) {
			this.waitingRequests.put(requestId, req);
		} else {
			LOG.info("Failed to send request {}, instruction cancelled", requestId, future.cause());
			req.setResult(OperationResults.UNSENT);
		}
	}

	protected final synchronized ListenableFuture<OperationResult> sendMessage(final Message message, final SRPID requestId,
			final Metadata metadata) {
		final io.netty.util.concurrent.Future<Void> f = this.session.sendMessage(message);
		final PCEPRequest req = new PCEPRequest(metadata);

		this.sendingRequests.put(requestId, req);

		f.addListener(new FutureListener<Void>() {
			@Override
			public void operationComplete(final io.netty.util.concurrent.Future<Void> future) {
				messageSendingComplete(requestId, future);
			}
		});

		return req.getFuture();
	}

	protected final synchronized void stateSynchronizationAchieved(final DataModificationTransaction trans) {
		if (this.synced) {
			LOG.debug("State synchronization achieved while synchronized, not updating state");
			return;
		}

		// Update synchronization flag
		this.synced = true;
		this.topologyAugmentBuilder.setPathComputationClient(this.pccBuilder.setStateSync(PccSyncState.Synchronized).build());
		final Node1 ta = this.topologyAugmentBuilder.build();
		trans.putOperationalData(this.topologyAugment, ta);
		LOG.debug("Peer data {} set to {}", this.topologyAugment, ta);

		// The node has completed synchronization, cleanup metadata no longer reported back
		this.nodeState.cleanupExcept(this.lsps.values());
		LOG.debug("Session {} achieved synchronized state", session);
	}

	protected final synchronized void addLsp(final PLSPID id, final PATHNAME name) {
		Preconditions.checkState(lsps.containsKey(id) == false);
		lsps.put(id, name);
	}

	protected final synchronized PATHNAME getLsp(final PLSPID id) {
		return lsps.get(id);
	}

	protected final synchronized PATHNAME removeLsp(final PLSPID id) {
		return lsps.remove(id);
	}

	protected final synchronized Metadata getLspMetadata(final PATHNAME name) {
		return this.nodeState.getLspMetadata(name);
	}

	protected final synchronized void updateLspMetadata(final PATHNAME name, final Metadata metadata) {
		this.nodeState.setLspMetadata(name, metadata);
	}

	abstract protected void onSessionUp(PCEPSession session, PathComputationClientBuilder pccBuilder);
}
