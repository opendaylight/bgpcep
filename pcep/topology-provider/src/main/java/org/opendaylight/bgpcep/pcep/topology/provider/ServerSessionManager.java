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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MessageHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.StatefulTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.topology.pcep.type.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 *
 */
final class ServerSessionManager implements SessionListenerFactory<PCEPSessionListener>, AutoCloseable {
	private static String createNodeId(final InetAddress addr) {
		return "pcc://" + addr.getHostAddress();
	}

	private final class SessionListener implements PCEPSessionListener {
		private final Map<SrpIdNumber, SettableFuture<OperationResult>> waitingRequests = new HashMap<>();
		private final Map<SrpIdNumber, SettableFuture<OperationResult>> sendingRequests = new HashMap<>();
		private final Map<PlspId, SymbolicPathName> lsps = new HashMap<>();
		private PathComputationClientBuilder pccBuilder;
		private InstanceIdentifier<Node1> topologyAugment;
		private InstanceIdentifier<Node> topologyNode;
		private Node1Builder topologyAugmentBuilder;
		private boolean ownsTopology = false;
		private boolean synced = false;
		private PCEPSession session;
		private long requestId = 1;
		private NodeId nodeId;

		final Node topologyNode(final DataModificationTransaction trans, final InetAddress address) {
			final String pccId = createNodeId(address);
			final Topology topo = (Topology) trans.readOperationalData(ServerSessionManager.this.topology);

			for (final Node n : topo.getNode()) {
				LOG.debug("Matching topology node {} to id {}", n, pccId);
				if (n.getNodeId().getValue().equals(pccId)) {
					return n;
				}
			}

			/*
			 * We failed to find a matching node. Let's create a dynamic one
			 * and note that we are the owner (so we clean it up afterwards).
			 */
			final NodeId id = new NodeId(pccId);
			final NodeKey nk = new NodeKey(id);
			final InstanceIdentifier<Node> nti = InstanceIdentifier.builder(ServerSessionManager.this.topology).child(Node.class, nk).toInstance();

			final Node ret = new NodeBuilder().setKey(nk).setNodeId(id).build();

			trans.putOperationalData(nti, ret);
			this.ownsTopology = true;
			this.topologyNode = nti;
			this.nodeId = id;
			return ret;
		}

		@Override
		public synchronized void onSessionUp(final PCEPSession session) {
			/*
			 * The session went up. Look up the router in Inventory model,
			 * create it if it is not there (marking that fact for later
			 * deletion), and mark it as synchronizing. Also create it in
			 * the topology model, with empty LSP list.
			 */
			final InetAddress peerAddress = session.getRemoteAddress();
			final DataModificationTransaction trans = ServerSessionManager.this.dataProvider.beginTransaction();

			final Node topoNode = topologyNode(trans, peerAddress);
			LOG.debug("Peer {} resolved to topology node {}", peerAddress, topoNode);

			// Our augmentation in the topology node
			this.pccBuilder = new PathComputationClientBuilder();

			final Tlvs tlvs = session.getRemoteTlvs();
			final Stateful stateful = tlvs.getStateful();
			if (stateful != null) {
				this.pccBuilder.setStatefulTlv(new StatefulTlvBuilder(tlvs).build());
				this.pccBuilder.setStateSync(PccSyncState.InitialResync);
			}

			this.topologyAugmentBuilder = new Node1Builder().setPathComputationClient(this.pccBuilder.build());
			this.topologyAugment = InstanceIdentifier.builder(this.topologyNode).augmentation(Node1.class).toInstance();
			trans.putOperationalData(this.topologyAugment, this.topologyAugmentBuilder.build());

			// All set, commit the modifications
			final ListenableFuture<RpcResult<TransactionStatus>> f = JdkFutureAdapters.listenInPoolThread(trans.commit());
			Futures.addCallback(f, new FutureCallback<RpcResult<TransactionStatus>>() {
				@Override
				public void onSuccess(final RpcResult<TransactionStatus> result) {
					// Nothing to do
				}

				@Override
				public void onFailure(final Throwable t) {
					LOG.error("Failed to update internal state for session {}, terminating it", session, t);
					session.close(TerminationReason.Unknown);
				}
			});

			ServerSessionManager.this.nodes.put(this.nodeId, this);
			this.session = session;
			LOG.info("Session with {} attached to topology node {}", session.getRemoteAddress(), topoNode.getNodeId());
		}

		@GuardedBy("this")
		private void tearDown(final PCEPSession session) {
			this.session = null;
			ServerSessionManager.this.nodes.remove(this.nodeId);

			final DataModificationTransaction trans = ServerSessionManager.this.dataProvider.beginTransaction();

			// The session went down. Undo all the Topology changes we have done.
			trans.removeOperationalData(this.topologyAugment);
			if (this.ownsTopology) {
				trans.removeOperationalData(this.topologyNode);
			}

			Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
				@Override
				public void onSuccess(final RpcResult<TransactionStatus> result) {
					// Nothing to do
				}

				@Override
				public void onFailure(final Throwable t) {
					LOG.error("Failed to cleanup internal state for session {}", session, t);
				}
			});

			// Clear all requests which have not been sent to the peer: they result in cancellation
			for (final Entry<SrpIdNumber, SettableFuture<OperationResult>> e : this.sendingRequests.entrySet()) {
				LOG.debug("Request {} was not sent when session went down, cancelling the instruction", e.getKey());
				e.getValue().set(OPERATION_UNSENT);
			}
			this.sendingRequests.clear();

			// CLear all requests which have not been acked by the peer: they result in failure
			for (final Entry<SrpIdNumber, SettableFuture<OperationResult>> e : this.waitingRequests.entrySet()) {
				LOG.info("Request {} was incomplete when session went down, failing the instruction", e.getKey());
				e.getValue().set(OPERATION_NOACK);
			}
			this.waitingRequests.clear();
		}

		@Override
		public synchronized void onSessionDown(final PCEPSession session, final Exception e) {
			LOG.warn("Session {} went down unexpectedly", e);
			tearDown(session);
		}

		@Override
		public synchronized void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason reason) {
			LOG.info("Session {} terminated by peer with reason {}", session, reason);
			tearDown(session);
		}

		private InstanceIdentifier<ReportedLsps> lspIdentifier(final SymbolicPathName name) {
			return InstanceIdentifier.builder(this.topologyAugment).child(PathComputationClient.class).child(ReportedLsps.class,
					new ReportedLspsKey(name.getPathName())).toInstance();
		}

		@Override
		public synchronized void onMessage(final PCEPSession session, final Message message) {
			if (!(message instanceof PcrptMessage)) {
				LOG.info("Unhandled message {} on session {}", message, session);
				session.sendMessage(unhandledMessageError);
			}

			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.PcrptMessage rpt = ((PcrptMessage) message).getPcrptMessage();

			final DataModificationTransaction trans = ServerSessionManager.this.dataProvider.beginTransaction();

			for (final Reports r : rpt.getReports()) {
				final Lsp lsp = r.getLsp();

				if (lsp.isSync() && !this.synced) {
					// Update synchronization flag
					this.synced = true;
					this.topologyAugmentBuilder.setPathComputationClient(this.pccBuilder.setStateSync(PccSyncState.Synchronized).build());
					trans.putOperationalData(this.topologyAugment, this.topologyAugmentBuilder.build());
					LOG.debug("Session {} achieved synchronized state", session);
				}

				final Srp srp = r.getSrp();
				if (srp != null) {
					final SrpIdNumber id = srp.getOperationId();
					if (id.getValue() != 0) {
						switch (lsp.getOperational()) {
						case Active:
						case Down:
						case Up:
							final SettableFuture<OperationResult> p = this.waitingRequests.remove(id);
							if (p != null) {
								LOG.debug("Request {} resulted in LSP operational state {}", id, lsp.getOperational());
								p.set(OPERATION_SUCCESS);
							} else {
								LOG.warn("Request ID {} not found in outstanding DB", id);
							}
							break;
						case GoingDown:
						case GoingUp:
							// These are transitive states, so
							break;
						}
					}
				}

				final PlspId id = lsp.getPlspId();
				if (lsp.isRemove()) {
					final SymbolicPathName name = this.lsps.remove(id);
					if (name != null) {
						trans.removeOperationalData(lspIdentifier(name));
					}

					LOG.debug("LSP {} removed", lsp);
				} else {
					if (!this.lsps.containsKey(id)) {
						LOG.debug("PLSPID {} not known yet, looking for a symbolic name", id);

						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.lsp.Tlvs tlvs = r.getLsp().getTlvs();
						final SymbolicPathName name = tlvs.getSymbolicPathName();
						if (name == null) {
							LOG.error("PLSPID {} seen for the first time, not reporting the LSP", id);
							// FIXME: BUG-189
							continue;
						}
						this.lsps.put(id, name);
					}

					final SymbolicPathName name = this.lsps.get(id);
					trans.putOperationalData(lspIdentifier(name), lsp);

					LOG.debug("LSP {} updated", lsp);
				}
			}

			Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
				@Override
				public void onSuccess(final RpcResult<TransactionStatus> result) {
					// Nothing to do
				}

				@Override
				public void onFailure(final Throwable t) {
					LOG.error("Failed to update internal state for session {}, closing it", session, t);
					session.close(TerminationReason.Unknown);
				}
			});
		}

		private synchronized SrpIdNumber nextRequest() {
			return new SrpIdNumber(this.requestId++);
		}

		private synchronized void messageSendingComplete(final SrpIdNumber requestId, final io.netty.util.concurrent.Future<Void> future) {
			final SettableFuture<OperationResult> promise = this.sendingRequests.remove(requestId);

			if (future.isSuccess()) {
				this.waitingRequests.put(requestId, promise);
			} else {
				LOG.info("Failed to send request {}, instruction cancelled", requestId, future.cause());
				promise.set(OPERATION_UNSENT);
			}
		}

		private synchronized ListenableFuture<OperationResult> sendMessage(final Message message, final SrpIdNumber requestId) {
			final io.netty.util.concurrent.Future<Void> f = this.session.sendMessage(message);
			final SettableFuture<OperationResult> ret = SettableFuture.create();

			this.sendingRequests.put(requestId, ret);

			f.addListener(new FutureListener<Void>() {
				@Override
				public void operationComplete(final io.netty.util.concurrent.Future<Void> future) {
					messageSendingComplete(requestId, future);
				}
			});

			return ret;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ServerSessionManager.class);
	private static final Pcerr unhandledMessageError = new PcerrBuilder().setPcerrMessage(
			new PcerrMessageBuilder().setErrorType(null).build()).build();
	private static final MessageHeader messageHeader = new MessageHeader() {
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

	private static final OperationResult OPERATION_NOACK = createOperationResult(FailureType.NoAck);
	private static final OperationResult OPERATION_SUCCESS = createOperationResult(null);
	private static final OperationResult OPERATION_UNSENT = createOperationResult(FailureType.Unsent);

	private final Map<NodeId, SessionListener> nodes = new HashMap<>();
	private final InstanceIdentifier<Topology> topology;
	private final DataProviderService dataProvider;

	public ServerSessionManager(final DataProviderService dataProvider, final InstanceIdentifier<Topology> topology) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.topology = Preconditions.checkNotNull(topology);

		// Make sure the topology does not exist
		final Object c = dataProvider.readOperationalData(topology);
		Preconditions.checkArgument(c == null, "Topology %s already exists", topology);

		// Now create the base topology
		final TopologyKey k = InstanceIdentifier.keyOf(topology);
		final DataModificationTransaction t = dataProvider.beginTransaction();
		t.putOperationalData(
				topology,
				new TopologyBuilder().setKey(k).setTopologyId(k.getTopologyId()).setTopologyTypes(
						new TopologyTypesBuilder().addAugmentation(TopologyTypes1.class,
								new TopologyTypes1Builder().setTopologyPcep(new TopologyPcepBuilder().build()).build()).build()).setNode(
										new ArrayList<Node>()).build());

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(t.commit()),
				new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				// Nothing to do
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to create topology {}", topology);
			}
		});
	}

	@Override
	public PCEPSessionListener getSessionListener() {
		return new SessionListener();
	}

	synchronized ListenableFuture<OperationResult> realAddLsp(final AddLspArgs input) {
		// Get the listener corresponding to the node
		final SessionListener l = this.nodes.get(input.getNode());
		if (l == null) {
			LOG.debug("Session for node {} not found", input.getNode());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Make sure there is no such LSP
		final InstanceIdentifier<ReportedLsps> lsp = InstanceIdentifier.builder(l.topologyAugment).child(PathComputationClient.class).child(
				ReportedLsps.class, new ReportedLspsKey(input.getName())).toInstance();
		if (this.dataProvider.readOperationalData(lsp) != null) {
			LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Build the request
		final RequestsBuilder rb = new RequestsBuilder((EndpointsObject) input.getArguments());
		rb.setSrp(new SrpBuilder().setOperationId(l.nextRequest()).setProcessingRule(Boolean.TRUE).build());
		rb.setLsp(new LspBuilder().setAdministrative(input.getArguments().isAdministrative()).setDelegate(Boolean.TRUE).setTlvs(
				new TlvsBuilder().setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(input.getName()).build()).build()).build());

		final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(messageHeader);
		ib.setRequests(ImmutableList.of(rb.build()));

		// Send the message
		return l.sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId());
	}

	private static OperationResult createOperationResult(final FailureType type) {
		return new OperationResult() {
			@Override
			public Class<? extends DataContainer> getImplementedInterface() {
				return OperationResult.class;
			}

			@Override
			public FailureType getFailure() {
				return type;
			}
		};
	}

	synchronized ListenableFuture<OperationResult> realRemoveLsp(final RemoveLspArgs input) {
		// Get the listener corresponding to the node
		final SessionListener l = this.nodes.get(input.getNode());
		if (l == null) {
			LOG.debug("Session for node {} not found", input.getNode());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Make sure the LSP exists, we need it for PLSP-ID
		final InstanceIdentifier<ReportedLsps> lsp = InstanceIdentifier.builder(l.topologyAugment).child(PathComputationClient.class).child(
				ReportedLsps.class, new ReportedLspsKey(input.getName())).toInstance();
		final ReportedLsps rep = (ReportedLsps) this.dataProvider.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Build the request and send it
		final RequestsBuilder rb = new RequestsBuilder();
		rb.setSrp(new SrpBuilder().setOperationId(l.nextRequest()).setProcessingRule(Boolean.TRUE).setFlags(new Flags(Boolean.TRUE)).build());
		rb.setLsp(new LspBuilder().setRemove(Boolean.TRUE).setPlspId(rep.getLsp().getPlspId()).setDelegate(Boolean.TRUE).build());

		final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(messageHeader);
		ib.setRequests(ImmutableList.of(rb.build()));
		return l.sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId());
	}

	synchronized ListenableFuture<OperationResult> realUpdateLsp(final UpdateLspArgs input) {
		// Get the listener corresponding to the node
		final SessionListener l = this.nodes.get(input.getNode());
		if (l == null) {
			LOG.debug("Session for node {} not found", input.getNode());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsps> lsp = InstanceIdentifier.builder(l.topologyAugment).child(PathComputationClient.class).child(
				ReportedLsps.class, new ReportedLspsKey(input.getName())).toInstance();
		final ReportedLsps rep = (ReportedLsps) this.dataProvider.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Build the PCUpd request and send it
		final UpdatesBuilder rb = new UpdatesBuilder();
		rb.setSrp(new SrpBuilder().setOperationId(l.nextRequest()).setProcessingRule(Boolean.TRUE).build());
		rb.setLsp(new LspBuilder().setPlspId(rep.getLsp().getPlspId()).setDelegate(Boolean.TRUE).build());
		final PathBuilder pb = new PathBuilder();
		rb.setPath(pb.setEro(input.getArguments().getEro()).build());

		final PcupdMessageBuilder ub = new PcupdMessageBuilder(messageHeader);
		ub.setUpdates(ImmutableList.of(rb.build()));
		return l.sendMessage(new PcupdBuilder().setPcupdMessage(ub.build()).build(), rb.getSrp().getOperationId());
	}

	synchronized ListenableFuture<OperationResult> realEnsureLspOperational(final EnsureLspOperationalInput input) {
		// Get the listener corresponding to the node
		final SessionListener l = this.nodes.get(input.getNode());
		if (l == null) {
			LOG.debug("Session for node {} not found", input.getNode());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsps> lsp = InstanceIdentifier.builder(l.topologyAugment).child(PathComputationClient.class).child(
				ReportedLsps.class, new ReportedLspsKey(input.getName())).toInstance();
		LOG.debug("Checking if LSP {} has operational state {}", lsp, input.getArguments().getOperational());
		final ReportedLsps rep = (ReportedLsps) this.dataProvider.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return Futures.immediateFuture(OPERATION_UNSENT);
		}

		if (rep.getLsp().getOperational().equals(input.getArguments().getOperational())) {
			return Futures.immediateFuture(OPERATION_SUCCESS);
		} else {
			return Futures.immediateFuture(OPERATION_UNSENT);
		}
	}

	@Override
	public void close() throws InterruptedException, ExecutionException {
		final DataModificationTransaction t = this.dataProvider.beginTransaction();
		t.removeOperationalData(this.topology);
		t.commit().get();
	}
}
