/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import java.net.InetAddress;
import java.util.Collections;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.StatefulTlvBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

final class Stateful07TopologySessionListener extends AbstractTopologySessionListener<SrpIdNumber, PlspId, SymbolicPathName> implements PCEPSessionListener {
	private static final Logger LOG = LoggerFactory.getLogger(Stateful07TopologySessionListener.class);

	/**
	 * @param serverSessionManager
	 */
	Stateful07TopologySessionListener(final ServerSessionManager serverSessionManager) {
		super(serverSessionManager);
	}

	@GuardedBy("this")
	private long requestId = 1;

	@Override
	protected void onSessionUp(final PCEPSession session, final PathComputationClientBuilder pccBuilder) {
		final InetAddress peerAddress = session.getRemoteAddress();

		final Tlvs tlvs = session.getRemoteTlvs();
		if (tlvs.getAugmentation(Tlvs2.class) != null) {
			final Stateful stateful = tlvs.getAugmentation(Tlvs2.class).getStateful();
			if (stateful != null) {
				pccBuilder.setReportedLsp(Collections.<ReportedLsp> emptyList());
				pccBuilder.setStatefulTlv(new StatefulTlvBuilder().setStateful(stateful).build());
				pccBuilder.setStateSync(PccSyncState.InitialResync);
			} else {
				LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
			}
		} else {
			LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
		}
	}

	private InstanceIdentifierBuilder<ReportedLsp> lspIdentifier(final ReportedLspKey key) {
		return pccIdentifier().child(ReportedLsp.class, key);
	}

	@Override
	public synchronized void onMessage(final PCEPSession session, final Message message) {
		if (!(message instanceof PcrptMessage)) {
			LOG.info("Unhandled message {} on session {}", message, session);
			session.sendMessage(UNHANDLED_MESSAGE_ERROR);
		}

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessage rpt = ((PcrptMessage) message).getPcrptMessage();

		final DataModificationTransaction trans = this.serverSessionManager.beginTransaction();

		for (final Reports r : rpt.getReports()) {
			final Lsp lsp = r.getLsp();

			if (!lsp.isSync()) {
				stateSynchronizationAchieved(trans);
				continue;
			}

			final ReportedLspBuilder rlb = new ReportedLspBuilder(r);
			boolean solicited = false;

			final Srp srp = r.getSrp();
			if (srp != null) {
				final SrpIdNumber id = srp.getOperationId();
				if (id.getValue() != 0) {
					solicited = true;

					switch (lsp.getOperational()) {
					case Active:
					case Down:
					case Up:
						final PCEPRequest req = removeRequest(id);
						if (req != null) {
							LOG.debug("Request {} resulted in LSP operational state {}", id, lsp.getOperational());
							rlb.setMetadata(req.getMetadata());
							req.setResult(OperationResults.SUCCESS);
						} else {
							LOG.warn("Request ID {} not found in outstanding DB", id);
						}
						break;
					case GoingDown:
					case GoingUp:
						// These are transitive states, so we don't have to do anything, as they will be followed
						// up...
						break;
					}
				}
			}

			final PlspId id = lsp.getPlspId();
			if (lsp.isRemove()) {
				final SymbolicPathName name = removeLsp(id);
				if (name != null) {
					trans.removeOperationalData(lspIdentifier(new ReportedLspKey(name)).build());
				}

				LOG.debug("LSP {} removed", lsp);
			} else {
				SymbolicPathName name = getLsp(id);
				if (name == null) {
					LOG.debug("PLSPID {} not known yet, looking for a symbolic name", id);

					final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs tlvs = r.getLsp().getTlvs();
					if (tlvs != null && tlvs.getSymbolicPathName() != null) {
						name = tlvs.getSymbolicPathName().getPathName();
						addLsp(id, name);
					} else {
						LOG.error("PLSPID {} seen for the first time, not reporting the LSP", id);
						continue;
					}
				}

				Preconditions.checkState(name != null);
				rlb.setKey(new ReportedLspKey(name));

				// If this is an unsolicited update. We need to make sure we retain the metadata already present
				if (solicited) {
					updateLspMetadata(name, rlb.getMetadata());
				} else {
					rlb.setMetadata(getLspMetadata(name));
				}

				trans.putOperationalData(lspIdentifier(rlb.getKey()).build(), rlb.build());

				LOG.debug("LSP {} updated", lsp);
			}
		}

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				LOG.trace("Internal state for session {} updated successfully", session);
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to update internal state for session {}, closing it", session, t);
				session.close(TerminationReason.Unknown);
			}
		});
	}

	@GuardedBy("this")
	private SrpIdNumber nextRequest() {
		return new SrpIdNumber(this.requestId++);
	}

	@Override
	public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
		// Make sure there is no such LSP
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(new ReportedLspKey(input.getName())).build();
		if (serverSessionManager.readOperationalData(lsp) != null) {
			LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
			return OperationResults.UNSENT.future();
		}

		// Build the request
		final RequestsBuilder rb = new RequestsBuilder();
		rb.fieldsFrom(input.getArguments());
		rb.setSrp(new SrpBuilder().setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build());
		rb.setLsp(new LspBuilder().setAdministrative(input.getArguments().isAdministrative()).setDelegate(Boolean.TRUE).setTlvs(
				new TlvsBuilder().setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(input.getName()).build()).build()).build());

		final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
		ib.setRequests(ImmutableList.of(rb.build()));

		// Send the message
		return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId(),
				input.getArguments().getMetadata());
	}

	@Override
	public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
		// Make sure the LSP exists, we need it for PLSP-ID
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(new ReportedLspKey(input.getName())).build();
		final ReportedLsp rep = serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		// Build the request and send it
		final RequestsBuilder rb = new RequestsBuilder();
		rb.setSrp(new SrpBuilder().setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build());
		rb.setLsp(new LspBuilder().setRemove(Boolean.TRUE).setPlspId(rep.getLsp().getPlspId()).setDelegate(Boolean.TRUE).build());

		final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
		ib.setRequests(ImmutableList.of(rb.build()));
		return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId(), null);
	}

	@Override
	public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(new ReportedLspKey(input.getName())).build();
		final ReportedLsp rep = serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		// Build the PCUpd request and send it
		final UpdatesBuilder rb = new UpdatesBuilder();
		rb.setSrp(new SrpBuilder().setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build());
		rb.setLsp(new LspBuilder().setPlspId(rep.getLsp().getPlspId()).setDelegate(Boolean.TRUE).build());
		final PathBuilder pb = new PathBuilder();
		rb.setPath(pb.setEro(input.getArguments().getEro()).build());

		final PcupdMessageBuilder ub = new PcupdMessageBuilder(MESSAGE_HEADER);
		ub.setUpdates(ImmutableList.of(rb.build()));
		return sendMessage(new PcupdBuilder().setPcupdMessage(ub.build()).build(), rb.getSrp().getOperationId(),
				input.getArguments().getMetadata());
	}

	@Override
	public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(new ReportedLspKey(input.getName())).build();
		LOG.debug("Checking if LSP {} has operational state {}", lsp, input.getArguments().getOperational());
		final ReportedLsp rep = serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		if (rep.getLsp().getOperational().equals(input.getArguments().getOperational())) {
			return OperationResults.SUCCESS.future();
		} else {
			return OperationResults.UNSENT.future();
		}
	}
}