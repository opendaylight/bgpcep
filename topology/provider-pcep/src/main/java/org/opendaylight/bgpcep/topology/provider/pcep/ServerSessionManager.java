/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.pcep;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.nodes.node.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.nodes.node.path.computation.client.StatefulTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.reports.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PccBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.pcc.Lsps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.pcc.LspsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
final class ServerSessionManager implements SessionListenerFactory<PCEPSessionListener> {
	private static String createNodeId(final InetAddress addr) {
		return "pcc://" + addr.getHostAddress();
	}

	private final class SessionListener implements PCEPSessionListener {
		private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node inventoryNode(
				final DataModificationTransaction trans, final InetAddress address) {
			final String pccId = createNodeId(address);

			// FIXME: after 0.6 yangtools, this cast should not be needed
			final Nodes nodes = (Nodes) trans.readOperationalData(ServerSessionManager.this.inventory);

			for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node n : nodes.getNode()) {
				LOG.debug("Matching inventory node {} to peer {}", n, address);
				if (n.getId().getValue().equals(pccId)) {
					return n;
				}

				// FIXME: locate the node by its management IP address
			}

			/*
			 * We failed to find a matching node. Let's create a dynamic one
			 * to have a backer. Note that this node will be created in the
			 * Runtime data space.
			 */
			LOG.debug("Failed to find inventory node for peer {}, creating a new one at {}", address, pccId);

			final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId id = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(pccId);
			final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey nk = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(id);
			final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nii = InstanceIdentifier.builder(
					ServerSessionManager.this.inventory).node(
					org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nk).toInstance();
			final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node ret = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder().setId(
					id).setKey(nk).build();

			trans.putRuntimeData(nii, ret);
			ServerSessionManager.this.ownsInventory = true;
			ServerSessionManager.this.inventoryNodeId = nii;
			return ret;
		}

		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node topologyNode(
				final DataModificationTransaction trans,
				final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node invNode) {
			// FIXME: after 0.6 yangtools, this cast should not be needed
			final Topology topo = (Topology) trans.readOperationalData(ServerSessionManager.this.topology);

			for (final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node n : topo.getNode()) {
				LOG.debug("Matching topology node {} to inventory node {}", n, invNode);
				if (n.getNodeId().getValue().equals(invNode.getId().getValue())) {
					return n;
				}
			}

			/*
			 * We failed to find a matching node. Let's create a dynamic one
			 * and note that we are the owner (so we clean it up afterwards).
			 */
			final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId id = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId(invNode.getId().getValue());
			final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey nk = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey(id);
			final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nti = InstanceIdentifier.builder(
					ServerSessionManager.this.topology).node(
					org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
					nk).toInstance();

			final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node ret = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder().setKey(
					nk).setNodeId(id).build();

			trans.putRuntimeData(nti, ret);
			ServerSessionManager.this.ownsTopology = true;
			ServerSessionManager.this.topologyNodeId = nti;
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

			final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node invNode = inventoryNode(trans, peerAddress);
			LOG.debug("Peer {} resolved to inventory node {}", peerAddress, invNode);

			final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node topoNode = topologyNode(
					trans, invNode);
			LOG.debug("Peer {} resolved to topology node {}", peerAddress, topoNode);

			// Our augmentation in the topology node
			final PccBuilder pb = new PccBuilder();

			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1 topoAugment = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1Builder().setPcc(
					pb.build()).build();
			ServerSessionManager.this.topologyAugmentId = InstanceIdentifier.builder(ServerSessionManager.this.topologyNodeId).node(
					org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1.class).toInstance();
			trans.putRuntimeData(ServerSessionManager.this.topologyAugmentId, topoAugment);

			// Our augmentation in the inventory node
			ServerSessionManager.this.pccBuilder = new PathComputationClientBuilder();

			final Tlvs tlvs = session.getRemoteTlvs();
			final Stateful stateful = tlvs.getStateful();
			if (stateful != null) {
				// FIXME: rework once groupings can be used in builders
				ServerSessionManager.this.pccBuilder.setStatefulTlv(new StatefulTlvBuilder().setFlags(tlvs.getStateful().getFlags()).build());
				ServerSessionManager.this.pccBuilder.setStateSync(PccSyncState.InitialResync);
			}

			ServerSessionManager.this.pccBuilder.setTopologyNode(topoNode.getNodeId());

			ServerSessionManager.this.inventoryAugmentBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.Node1Builder().setPathComputationClient(ServerSessionManager.this.pccBuilder.build());
			ServerSessionManager.this.inventoryAugmentId = InstanceIdentifier.builder(ServerSessionManager.this.inventoryNodeId).node(
					org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.Node1.class).toInstance();
			trans.putRuntimeData(ServerSessionManager.this.inventoryAugmentId, ServerSessionManager.this.inventoryAugmentBuilder.build());

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

			LOG.info("Session with {} attached to inventory node {} and topology node {}", session.getRemoteAddress(), invNode.getId(),
					topoNode.getNodeId());
		}

		private void tearDown(final PCEPSession session) {
			final DataModificationTransaction trans = ServerSessionManager.this.dataProvider.beginTransaction();

			/*
			 * The session went down. Undo all the Inventory and Topology
			 * changes we have done.
			 */
			trans.removeRuntimeData(ServerSessionManager.this.inventoryAugmentId);
			if (ServerSessionManager.this.ownsInventory) {
				trans.removeRuntimeData(ServerSessionManager.this.inventoryNodeId);
			}
			trans.removeRuntimeData(ServerSessionManager.this.topologyAugmentId);
			if (ServerSessionManager.this.ownsTopology) {
				trans.removeRuntimeData(ServerSessionManager.this.topologyNodeId);
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

		private InstanceIdentifier<Lsps> lspIdentifier(final SymbolicPathName name) {
			return InstanceIdentifier.builder(ServerSessionManager.this.topologyAugmentId).node(Lsps.class, new LspsKey(name.getPathName())).toInstance();
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

				if (lsp.isSync() && !ServerSessionManager.this.synced) {
					// Update synchronization flag
					ServerSessionManager.this.synced = true;
					ServerSessionManager.this.inventoryAugmentBuilder.setPathComputationClient(ServerSessionManager.this.pccBuilder.setStateSync(
							PccSyncState.Synchronized).build());
					trans.putRuntimeData(ServerSessionManager.this.inventoryAugmentId,
							ServerSessionManager.this.inventoryAugmentBuilder.build());
					LOG.debug("Session {} achieved synchronized state", session);
				}

				final PlspId id = lsp.getPlspId();
				if (lsp.isRemove()) {
					final SymbolicPathName name = ServerSessionManager.this.lsps.remove(id);
					if (name != null) {
						trans.removeRuntimeData(lspIdentifier(name));
					}

					LOG.debug("LSP {} removed", lsp);
				} else {
					if (!ServerSessionManager.this.lsps.containsKey(id)) {
						LOG.debug("PLSPID {} not known yet, looking for a symbolic name", id);

						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Tlvs tlvs = r.getLsp().getTlvs();
						final SymbolicPathName name = tlvs.getSymbolicPathName();
						if (name == null) {
							LOG.error("PLSPID {} seen for the first time, not reporting the LSP");
							// TODO: what should we do here?
							continue;
						}
					}

					final SymbolicPathName name = ServerSessionManager.this.lsps.get(id);
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
	private final Map<PlspId, SymbolicPathName> lsps = new HashMap<>();
	private final InstanceIdentifier<Nodes> inventory;
	private final InstanceIdentifier<Topology> topology;
	private final DataProviderService dataProvider;

	private boolean ownsInventory = false;
	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> inventoryNodeId;
	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.Node1> inventoryAugmentId;
	private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.pcep.rev131024.Node1Builder inventoryAugmentBuilder;
	private PathComputationClientBuilder pccBuilder;
	private boolean synced = false;

	private boolean ownsTopology = false;
	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> topologyNodeId;
	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1> topologyAugmentId;

	public ServerSessionManager(final DataProviderService dataProvider, final InstanceIdentifier<Nodes> inventory,
			final InstanceIdentifier<Topology> topology) {
		this.dataProvider = Preconditions.checkNotNull(dataProvider);
		this.inventory = Preconditions.checkNotNull(inventory);
		this.topology = Preconditions.checkNotNull(topology);
	}

	@Override
	public PCEPSessionListener getSessionListener() {
		return new SessionListener();
	}
}
