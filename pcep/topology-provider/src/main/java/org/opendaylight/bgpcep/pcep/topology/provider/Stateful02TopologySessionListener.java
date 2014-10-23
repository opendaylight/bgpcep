/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PeerCapabilities;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.LspId;
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

public class Stateful02TopologySessionListener extends AbstractTopologySessionListener<String, PlspId> {
    private static final Logger LOG = LoggerFactory.getLogger(Stateful02TopologySessionListener.class);

    /**
     * @param serverSessionManager
     */
    Stateful02TopologySessionListener(final ServerSessionManager serverSessionManager) {
        super(serverSessionManager);
    }

    @Override
    protected void onSessionUp(final PCEPSession session, final PathComputationClientBuilder pccBuilder) {
        final InetAddress peerAddress = session.getRemoteAddress();

        final Tlvs tlvs = session.getRemoteTlvs();
        final Tlvs2 tlv = tlvs.getAugmentation(Tlvs2.class);
        if (tlv != null) {
            final Stateful stateful = tlv.getStateful();
            if (stateful != null) {
                getSessionListenerState().setPeerCapabilities(getCapabilities(stateful));
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
    protected synchronized boolean onMessage(final MessageContext ctx, final Message message) {
        if (!(message instanceof PcrptMessage)) {
            return true;
        }

        getSessionListenerState().updateLastReceivedRptMsg();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.PcrptMessage rpt = ((PcrptMessage) message).getPcrptMessage();
        for (final Reports r : rpt.getReports()) {
            final Lsp lsp = r.getLsp();
            if (lsp == null) {
                LOG.warn("PCRpt message received without LSP object.");
                return true;
            }

            final PlspId id = lsp.getPlspId();
            if (!lsp.isSync() && (id == null || id.getValue() == 0)) {
                stateSynchronizationAchieved(ctx);
                continue;
            }

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.Tlvs tlvs = lsp.getTlvs();
            final String name = (tlvs != null && tlvs.getSymbolicPathName() != null) ?
                Charsets.UTF_8.decode(ByteBuffer.wrap(tlvs.getSymbolicPathName().getPathName().getValue())).toString()
                : lookupLspName(id);

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.PathBuilder pb = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.PathBuilder();
            // set 0 by default, as the first report does not contain LSPIdentifiersTLV
            pb.setLspId(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId(0L));
            if (r.getPath() != null) {
                pb.fieldsFrom(r.getPath());
            }

            final ReportedLspBuilder rlb = new ReportedLspBuilder();
            rlb.addAugmentation(ReportedLsp1.class, new ReportedLsp1Builder().setLsp(lsp).build());
            rlb.setPath(Collections.singletonList(pb.build()));
            boolean solicited = false;

            if (id.getValue() != 0) {
                solicited = true;
                final PCEPRequest req = removeRequest(name);
                if (req != null) {
                    LOG.debug("Request {} resulted in LSP operational state {}", id, lsp.isOperational());
                    rlb.setMetadata(req.getMetadata());
                    ctx.resolveRequest(req);
                } else {
                    LOG.warn("Request ID {} not found in outstanding DB", id);
                }
            }

            if (!lsp.isRemove()) {
                updateLsp(ctx, id, name, rlb, solicited, false);
                LOG.debug("LSP {} updated", lsp);
            } else {
                removeLsp(ctx, id);
                LOG.debug("LSP {} removed", lsp);
            }
        }

        return false;
    }

    @Override
    public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        // Make sure there is no such LSP
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                if (rep.isPresent()) {
                    LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
                    return OperationResults.createUnsent(PCEPErrors.USED_SYMBOLIC_PATH_NAME).future();
                }

                final SymbolicPathNameBuilder name = new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(input.getName().getBytes(Charsets.UTF_8)));

                // Build the request
                final RequestsBuilder rb = new RequestsBuilder();
                rb.fieldsFrom(input.getArguments());
                rb.setLspa(new LspaBuilder().setTlvs(
                    new TlvsBuilder().addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2Builder().setSymbolicPathName(
                            name.build()).build()).build()).build());

                final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
                ib.setRequests(Collections.singletonList(rb.build()));

                // Send the message
                return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), input.getName(),
                    input.getArguments().getMetadata());
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null, MISSING_XML_TAG);
        // Make sure the LSP exists, we need it for PLSP-ID
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                final Lsp reportedLsp = validateReportedLsp(rep, input);
                if (reportedLsp == null) {
                    LOG.warn("Reported LSP does not contain LSP object.");
                    return OperationResults.createUnsent(PCEPErrors.UNKNOWN_PLSP_ID).future();
                }
                // Build the request and send it
                final UpdatesBuilder rb = new UpdatesBuilder();
                rb.setLsp(new LspBuilder().setRemove(Boolean.TRUE).setPlspId(reportedLsp.getPlspId()).setDelegate(reportedLsp.isDelegate()).build());
                final PcupdMessageBuilder ib = new PcupdMessageBuilder(MESSAGE_HEADER);
                ib.setUpdates(Collections.singletonList(rb.build()));
                return sendMessage(new PcupdBuilder().setPcupdMessage(ib.build()).build(), rep.get().getName(), null);
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        // Make sure the LSP exists
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                final Lsp reportedLsp = validateReportedLsp(rep, input);
                if (reportedLsp == null) {
                    return OperationResults.createUnsent(PCEPErrors.UNKNOWN_PLSP_ID).future();
                }
                final Arguments2 args = input.getArguments().getAugmentation(Arguments2.class);
                Preconditions.checkArgument(args != null, "Input is missing operational tag.");
                Preconditions.checkArgument(input.getArguments().getEro() != null, "Input is missing ERO object.");
                // Build the PCUpd request and send it
                final UpdatesBuilder rb = new UpdatesBuilder();
                rb.setLsp(new LspBuilder().setPlspId(reportedLsp.getPlspId()).setDelegate(reportedLsp.isDelegate()).setOperational(args.isOperational()).build());
                final PathBuilder pb = new PathBuilder();
                pb.fieldsFrom(input.getArguments());
                rb.setPath(pb.build());
                final PcupdMessageBuilder ub = new PcupdMessageBuilder(MESSAGE_HEADER);
                ub.setUpdates(Collections.singletonList(rb.build()));
                return sendMessage(new PcupdBuilder().setPcupdMessage(ub.build()).build(), rep.get().getName(), input.getArguments().getMetadata());
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        final Boolean op;
        final Arguments1 aa = input.getArguments().getAugmentation(Arguments1.class);
        if (aa == null) {
            LOG.warn("Operational status not present in MD-SAL.");
            op = null;
        } else {
            op = aa.isOperational();
        }

        // Make sure the LSP exists
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        LOG.debug("Checking if LSP {} has operational state {}", lsp, op);
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new Function<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public OperationResult apply(final Optional<ReportedLsp> rep) {
                final Lsp reportedLsp = validateReportedLsp(rep, input);
                if (reportedLsp == null) {
                    return OperationResults.UNSENT;
                }
                return reportedLsp.isOperational().equals(op) ? OperationResults.SUCCESS : OperationResults.UNSENT;
            }
        });
    }

    @Override
    protected Lsp validateReportedLsp(final Optional<ReportedLsp> rep, final LspId input) {
        if (!rep.isPresent()) {
            LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
            return null;
        }
        final ReportedLsp1 ra = rep.get().getAugmentation(ReportedLsp1.class);
        if (ra == null) {
            LOG.warn("Reported LSP reported null from data-store.");
            return null;
        }
        final Lsp reportedLsp = ra.getLsp();
        Preconditions.checkState(reportedLsp != null, "Reported LSP does not contain LSP object.");
        return reportedLsp;
    }

    private static PeerCapabilities getCapabilities(final Stateful stateful) {
        final PeerCapabilities capa = new PeerCapabilities();
        capa.setStateful(true);
        if (stateful.isLspUpdateCapability() != null) {
            capa.setActive(stateful.isLspUpdateCapability());
        }
        final Stateful1 stateful1 = stateful.getAugmentation(Stateful1.class);
        if (stateful1 != null && stateful1.isInitiation() != null) {
            capa.setInstantiation(stateful1.isInitiation());
        }
        return capa;
    }
}
