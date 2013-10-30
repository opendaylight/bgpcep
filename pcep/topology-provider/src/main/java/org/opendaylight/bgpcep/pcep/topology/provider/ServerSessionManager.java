/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SuccessfulRpcResult;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.reports.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.FailureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.submit.instruction.output.result.failure.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.StatefulTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
final class ServerSessionManager implements SessionListenerFactory<PCEPSessionListener>, NetworkTopologyPcepService {
	private static String createNodeId(final InetAddress addr) {
		return "pcc://" + addr.getHostAddress();
	}

	private final class SessionListener implements PCEPSessionListener {
		private final Map<PlspId, SymbolicPathName> lsps = new HashMap<>();
		private PathComputationClientBuilder pccBuilder;
		private boolean synced = false;

		private boolean ownsTopology = false;
		private InstanceIdentifier<Node> topologyNodeId;
		private InstanceIdentifier<Node1> topologyAugmentId;
		private Node1Builder topologyAugmentBuilder;

		final Node topologyNode(final DataModificationTransaction trans, final InetAddress address) {
			final String pccId = createNodeId(address);

			// FIXME: after 0.6 yangtools, this cast should not be needed
			final Topology topo = (Topology)trans.readOperationalData(topology);

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
			final InstanceIdentifier<Node> nti = InstanceIdentifier.builder(topology).node(Node.class, nk).toInstance();

			final Node ret = new NodeBuilder().setKey(nk).setNodeId(id).build();

			trans.putRuntimeData(nti, ret);
			this.ownsTopology = true;
			this.topologyNodeId = nti;
			return ret;
		}

		@Override
		public void onSessionUp(final PCEPSession session) {
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
			pccBuilder = new PathComputationClientBuilder();

			final Tlvs tlvs = session.getRemoteTlvs();
			final Stateful stateful = tlvs.getStateful();
			if (stateful != null) {
				// FIXME: rework once groupings can be used in builders
				this.pccBuilder.setStatefulTlv(new StatefulTlvBuilder().setStateful(stateful).build());
				this.pccBuilder.setStateSync(PccSyncState.InitialResync);
			}

			topologyAugmentBuilder = new Node1Builder().setPathComputationClient(pccBuilder.build());
			topologyAugmentId = InstanceIdentifier.builder(topologyNodeId).node(Node1.class).toInstance();
			trans.putRuntimeData(topologyAugmentId, topologyAugmentBuilder.build());

			// All set, commit the modifications
			final Future<RpcResult<TransactionStatus>> s = trans.commit();

			/*
			 * FIXME: once this Future is listenable, attach to it so we can
			 *        do cleanup if the commit fails. For now we force a commit.
			 */
			try {
				s.get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Failed to update internal state for session {}, terminating it", session, e);
				session.close(TerminationReason.Unknown);
			}

			LOG.info("Session with {} attached to topology node {}", session.getRemoteAddress(), topoNode.getNodeId());
		}

		private void tearDown(final PCEPSession session) {
			final DataModificationTransaction trans = ServerSessionManager.this.dataProvider.beginTransaction();

			// The session went down. Undo all the Topology changes we have done.
			trans.removeRuntimeData(topologyAugmentId);
			if (ownsTopology) {
				trans.removeRuntimeData(topologyNodeId);
			}

			/*
			 * FIXME: once this Future is listenable, attach to it so we can
			 *        do cleanup if the commit fails. For now we force a commit.
			 */
			final Future<RpcResult<TransactionStatus>> s = trans.commit();
			try {
				s.get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Failed to cleanup internal state for session {}", session, e);
			}
		}

		@Override
		public void onSessionDown(final PCEPSession session, final Exception e) {
			LOG.warn("Session {} went down unexpectedly", e);
			tearDown(session);
		}

		@Override
		public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason reason) {
			LOG.info("Session {} terminated by peer with reason {}", session, reason);
			tearDown(session);
		}

		private InstanceIdentifier<ReportedLsps> lspIdentifier(final SymbolicPathName name) {
			return InstanceIdentifier.builder(topologyAugmentId).
					node(ReportedLsps.class, new ReportedLspsKey(name.getPathName())).toInstance();
		}

		@Override
		public void onMessage(final PCEPSession session, final Message message) {
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
					synced = true;
					topologyAugmentBuilder.setPathComputationClient(pccBuilder.setStateSync(PccSyncState.Synchronized).build());
					trans.putRuntimeData(topologyAugmentId, topologyAugmentBuilder.build());
					LOG.debug("Session {} achieved synchronized state", session);
				}

				final PlspId id = lsp.getPlspId();
				if (lsp.isRemove()) {
					final SymbolicPathName name = this.lsps.remove(id);
					if (name != null) {
						trans.removeRuntimeData(lspIdentifier(name));
					}

					LOG.debug("LSP {} removed", lsp);
				} else {
					if (!this.lsps.containsKey(id)) {
						LOG.debug("PLSPID {} not known yet, looking for a symbolic name", id);

						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Tlvs tlvs = r.getLsp().getTlvs();
						final SymbolicPathName name = tlvs.getSymbolicPathName();
						if (name == null) {
							LOG.error("PLSPID {} seen for the first time, not reporting the LSP");
							// TODO: what should we do here?
							continue;
						}
					}

					final SymbolicPathName name = this.lsps.get(id);
					trans.putRuntimeData(lspIdentifier(name), lsp);

					LOG.debug("LSP {} updated");
				}
			}

			/*
			 * FIXME: once this Future is listenable, attach to it so we can
			 *        do cleanup if the commit fails. For now we force a commit.
			 */
			final Future<RpcResult<TransactionStatus>> s = trans.commit();
			try {
				s.get();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Failed to update internal state for session {}, closing it", session, e);
				session.close(TerminationReason.Unknown);
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ServerSessionManager.class);
	private static final Pcerr unhandledMessageError = new PcerrBuilder().setPcerrMessage(
			new PcerrMessageBuilder().setErrorType(null).build()).build();
	private static final EventExecutor exec = GlobalEventExecutor.INSTANCE;
	private final InstanceIdentifier<Topology> topology;
	private final DataProviderService dataProvider;
	private final InstructionScheduler scheduler;

	public ServerSessionManager(final InstructionScheduler scheduler,
			final DataProviderService dataProvider, final InstanceIdentifier<Topology> topology) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.topology = Preconditions.checkNotNull(topology);
		this.scheduler = Preconditions.checkNotNull(scheduler);
	}

	@Override
	public PCEPSessionListener getSessionListener() {
		return new SessionListener();
	}

	private synchronized io.netty.util.concurrent.Future<ExecutionResult<?>> realAddLsp(final AddLspInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	private synchronized io.netty.util.concurrent.Future<ExecutionResult<?>> realRemoveLsp(final RemoveLspInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	private synchronized io.netty.util.concurrent.Future<ExecutionResult<?>> realUpdateLsp(final UpdateLspInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<AddLspOutput>> addLsp(final AddLspInput input) {
		final InstructionExecutor e = new InstructionExecutor() {
			@Override
			public io.netty.util.concurrent.Future<ExecutionResult<?>> execute() {
				return realAddLsp(input);
			}
		};

		final Failure f = scheduler.submitInstruction(input, e);
		final AddLspOutputBuilder b = new AddLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<AddLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}

	@Override
	public Future<RpcResult<RemoveLspOutput>> removeLsp(final RemoveLspInput input) {
		final InstructionExecutor e = new InstructionExecutor() {
			@Override
			public io.netty.util.concurrent.Future<ExecutionResult<?>> execute() {
				return realRemoveLsp(input);
			}
		};

		final Failure f = scheduler.submitInstruction(input, e);
		final RemoveLspOutputBuilder b = new RemoveLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<RemoveLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}

	@Override
	public Future<RpcResult<UpdateLspOutput>> updateLsp(final UpdateLspInput input) {
		final InstructionExecutor e = new InstructionExecutor() {
			@Override
			public io.netty.util.concurrent.Future<ExecutionResult<?>> execute() {
				return realUpdateLsp(input);
			}
		};

		final Failure f = scheduler.submitInstruction(input, e);
		final UpdateLspOutputBuilder b = new UpdateLspOutputBuilder();
		if (f != null) {
			b.setResult(new FailureBuilder().setFailure(f).build());
		}

		final RpcResult<UpdateLspOutput> res = SuccessfulRpcResult.create(b.build());
		return exec.newSucceededFuture(res);
	}
}
