/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.ReportedLsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.ReportedLsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.StatefulTlv1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.StatefulTlv1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.StatefulTlvBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

public class Stateful02TopologySessionListener extends AbstractTopologySessionListener<String, PlspId> {
	private static final Logger LOG = LoggerFactory.getLogger(Stateful02TopologySessionListener.class);

	/**
	 * @param serverSessionManager
	 */
	Stateful02TopologySessionListener(final ServerSessionManager serverSessionManager) {
		super(serverSessionManager);
	}

	@GuardedBy("this")
	private long requestId = 1;

	@Override
	protected void onSessionUp(final PCEPSession session, final PathComputationClientBuilder pccBuilder) {
		final InetAddress peerAddress = session.getRemoteAddress();

		final Tlvs tlvs = session.getRemoteTlvs();
		final Tlvs2 tlv = tlvs.getAugmentation(Tlvs2.class);
		if (tlv != null) {
			final Stateful stateful = tlv.getStateful();
			if (stateful != null) {
				pccBuilder.setReportedLsp(Collections.<ReportedLsp> emptyList());
				pccBuilder.setStateSync(PccSyncState.InitialResync);
				pccBuilder.setStatefulTlv(new StatefulTlvBuilder().addAugmentation(StatefulTlv1.class, new StatefulTlv1Builder(tlv).build()).build());
			} else {
				LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
			}
		} else {
			LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
		}
	}

	@Override
	protected synchronized boolean onMessage(final DataModificationTransaction trans, final Message message) {
		if (!(message instanceof PcrptMessage)) {
			return true;
		}

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.PcrptMessage rpt = ((PcrptMessage) message).getPcrptMessage();
		for (final Reports r : rpt.getReports()) {
			final Lsp lsp = r.getLsp();

			final PlspId id = lsp.getPlspId();
			if (!lsp.isSync() && (id == null || id.getValue() == 0)) {
				stateSynchronizationAchieved(trans);
				continue;
			}

			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.Tlvs tlvs = r.getLsp().getTlvs();
			final String name;
			if (tlvs != null && tlvs.getSymbolicPathName() != null) {
				name = Charsets.UTF_8.decode(ByteBuffer.wrap(tlvs.getSymbolicPathName().getPathName().getValue())).toString();
			} else {
				name = lookupLspName(id);
			}

			final ReportedLspBuilder rlb = new ReportedLspBuilder();
			rlb.addAugmentation(ReportedLsp1.class, new ReportedLsp1Builder().setLsp(r.getLsp()).build());
			boolean solicited = false;

			if (id.getValue() != 0) {
				solicited = true;

				final PCEPRequest req = removeRequest(name);
				if (req != null) {
					LOG.debug("Request {} resulted in LSP operational state {}", id, lsp.isOperational());
					rlb.setMetadata(req.getMetadata());
					req.setResult(OperationResults.SUCCESS);
				} else {
					LOG.warn("Request ID {} not found in outstanding DB", id);
				}
			}

			if (!lsp.isRemove()) {
				updateLsp(trans, id, name, rlb, solicited);
				LOG.debug("LSP {} updated", lsp);
			} else {
				removeLsp(trans, id);
				LOG.debug("LSP {} removed", lsp);
			}
		}

		return false;
	}

	@GuardedBy("this")
	private PlspId nextRequest() {
		return new PlspId(this.requestId++);
	}

	@Override
	public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
		// Make sure there is no such LSP
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName()).build();
		if (this.serverSessionManager.readOperationalData(lsp) != null) {
			LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
			return OperationResults.UNSENT.future();
		}

		final SymbolicPathNameBuilder name = new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(input.getName().getBytes()));

		// Build the request
		final RequestsBuilder rb = new RequestsBuilder();
		rb.fieldsFrom(input.getArguments());
		rb.setLspa(new LspaBuilder().setTlvs(
				new TlvsBuilder().addAugmentation(
						org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2.class,
						new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2Builder().setSymbolicPathName(
								name.build()).build()).build()).build());

		final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
		ib.setRequests(ImmutableList.of(rb.build()));

		// Send the message
		return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), input.getName(),
				input.getArguments().getMetadata());
	}

	@Override
	public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
		// Make sure the LSP exists, we need it for PLSP-ID
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName()).build();
		final ReportedLsp rep = this.serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		final ReportedLsp1 ra = rep.getAugmentation(ReportedLsp1.class);
		Preconditions.checkState(ra != null);

		// Build the request and send it
		final UpdatesBuilder rb = new UpdatesBuilder();
		rb.setLsp(new LspBuilder().setRemove(Boolean.TRUE).setPlspId(ra.getLsp().getPlspId()).setDelegate(Boolean.TRUE).build());

		final PcupdMessageBuilder ib = new PcupdMessageBuilder(MESSAGE_HEADER);
		ib.setUpdates(ImmutableList.of(rb.build()));
		return sendMessage(new PcupdBuilder().setPcupdMessage(ib.build()).build(), rep.getName(), null);
	}

	@Override
	public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName()).build();
		final ReportedLsp rep = this.serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.warn("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		final ReportedLsp1 ra = rep.getAugmentation(ReportedLsp1.class);
		Preconditions.checkState(ra != null);

		// Build the PCUpd request and send it
		final UpdatesBuilder rb = new UpdatesBuilder();
		rb.setLsp(new LspBuilder().setPlspId(ra.getLsp().getPlspId()).setDelegate(Boolean.TRUE).setOperational(input.getArguments().getAugmentation(Arguments2.class).isOperational()).build());
		final PathBuilder pb = new PathBuilder();
		rb.setPath(pb.setEro(input.getArguments().getEro()).build());

		final PcupdMessageBuilder ub = new PcupdMessageBuilder(MESSAGE_HEADER);
		ub.setUpdates(ImmutableList.of(rb.build()));
		return sendMessage(new PcupdBuilder().setPcupdMessage(ub.build()).build(), rep.getName(), input.getArguments().getMetadata());
	}

	@Override
	public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
		Boolean op = null;
		final Arguments1 aa = input.getArguments().getAugmentation(Arguments1.class);
		if (aa == null) {
			LOG.warn("Operational status not present in MD-SAL.");
		} else {
			op = aa.isOperational();
		}

		// Make sure the LSP exists
		final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName()).build();
		LOG.debug("Checking if LSP {} has operational state {}", lsp, op);
		final ReportedLsp rep = this.serverSessionManager.readOperationalData(lsp);
		if (rep == null) {
			LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		final ReportedLsp1 ra = rep.getAugmentation(ReportedLsp1.class);
		if (ra == null) {
			LOG.warn("Node {} LSP {} does not contain data", input.getNode(), input.getName());
			return OperationResults.UNSENT.future();
		}

		if (ra.getLsp().isOperational().equals(op)) {
			return OperationResults.SUCCESS.future();
		} else {
			return OperationResults.UNSENT.future();
		}
	}
}
