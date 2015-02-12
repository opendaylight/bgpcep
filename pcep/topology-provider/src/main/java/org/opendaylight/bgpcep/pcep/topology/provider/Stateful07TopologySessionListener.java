/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Path1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.StatefulTlv1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.StatefulTlv1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.StatefulCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
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

final class Stateful07TopologySessionListener extends AbstractTopologySessionListener<SrpIdNumber, PlspId> {
    private static final Logger LOG = LoggerFactory.getLogger(Stateful07TopologySessionListener.class);

    private final AtomicLong requestId = new AtomicLong(1L);

    /**
     * @param serverSessionManager
     */
    Stateful07TopologySessionListener(final ServerSessionManager serverSessionManager) {
        super(serverSessionManager);
    }

    @Override
    protected void onSessionUp(final PCEPSession session, final PathComputationClientBuilder pccBuilder) {
        final InetAddress peerAddress = session.getRemoteAddress();

        final Tlvs tlvs = session.getRemoteTlvs();
        if (tlvs != null && tlvs.getAugmentation(Tlvs1.class) != null) {
            final Stateful stateful = tlvs.getAugmentation(Tlvs1.class).getStateful();
            if (stateful != null) {
                pccBuilder.setReportedLsp(Collections.<ReportedLsp> emptyList());
                pccBuilder.setStateSync(PccSyncState.InitialResync);
                pccBuilder.setStatefulTlv(new StatefulTlvBuilder().addAugmentation(StatefulTlv1.class,
                    new StatefulTlv1Builder(tlvs.getAugmentation(Tlvs1.class)).build()).build());
            } else {
                LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
            }
        } else {
            LOG.debug("Peer {} does not advertise stateful TLV", peerAddress);
        }
    }

    @Override
    protected boolean onMessage(final MessageContext ctx, final Message message) {
        if (message instanceof PcerrMessage) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessage errMsg = ((PcerrMessage) message).getPcerrMessage();
            if (errMsg.getErrorType() instanceof StatefulCase) {
                final StatefulCase stat = (StatefulCase)errMsg.getErrorType();
                for (final Srps srps : stat.getStateful().getSrps()) {
                    final SrpIdNumber id = srps.getSrp().getOperationId();
                    if (id.getValue() != 0) {
                        final PCEPRequest req = removeRequest(id);
                        if (req != null) {
                            req.done(OperationResults.createFailed(errMsg.getErrors()));
                        } else {
                            LOG.warn("Request ID {} not found in outstanding DB", id);
                        }
                    }
                }
            } else {
                LOG.warn("Unhandled PCErr message {}.", errMsg);
                return true;
            }
            return false;
        }
        if (!(message instanceof PcrptMessage)) {
            return true;
        }

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessage rpt = ((PcrptMessage) message).getPcrptMessage();
        for (final Reports report : rpt.getReports()) {
            final Lsp lsp = report.getLsp();
            final PlspId plspid = lsp.getPlspId();

            if (!lsp.isSync() && (lsp.getPlspId() == null || plspid.getValue() == 0)) {
                stateSynchronizationAchieved(ctx);
                continue;
            }

            final ReportedLspBuilder rlb = new ReportedLspBuilder();

            boolean solicited = false;

            final Srp srp = report.getSrp();
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
                            ctx.resolveRequest(req);
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
                    // if remove flag is set in SRP object, remove the tunnel immediately
                    if (srp.getAugmentation(Srp1.class) != null) {
                        final Srp1 initiatedSrp = srp.getAugmentation(Srp1.class);
                        if (initiatedSrp.isRemove()) {
                            super.removeLsp(ctx, plspid);
                            return false;
                        }
                    }
                }
            }
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs tlvs = report.getLsp().getTlvs();
            final String name;
            if (tlvs != null && tlvs.getSymbolicPathName() != null) {
                name = Charsets.UTF_8.decode(ByteBuffer.wrap(tlvs.getSymbolicPathName().getPathName().getValue())).toString();
            } else {
                name = null;
            }
            LspId lspid = null;
            if (tlvs != null && tlvs.getLspIdentifiers() != null) {
                lspid = tlvs.getLspIdentifiers().getLspId();
            }
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.PathBuilder pb = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.PathBuilder();
            if (report.getPath() != null) {
                pb.fieldsFrom(report.getPath());
            }
            // LSP is mandatory (if there is none, parser will throw an exception)
            // this is to ensure a path will be created at any rate
            pb.addAugmentation(Path1.class, new Path1Builder().setLsp(report.getLsp()).build());
            pb.setLspId(lspid);
            rlb.setPath(Collections.singletonList(pb.build()));
            updateLsp(ctx, plspid, name, rlb, solicited, lsp.isRemove());
            LOG.debug("LSP {} updated", lsp);
        }
        return false;
    }

    private SrpIdNumber nextRequest() {
        return new SrpIdNumber(this.requestId.getAndIncrement());
    }

    @Override
    public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        LOG.trace("AddLspArgs {}", input);
        // Make sure there is no such LSP
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                if (rep.isPresent()) {
                    LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
                    return OperationResults.UNSENT.future();
                }

                // Build the request
                final RequestsBuilder rb = new RequestsBuilder();
                final Arguments2 args = input.getArguments().getAugmentation(Arguments2.class);
                Preconditions.checkArgument(args != null, "Input is missing operational tag.");
                final Lsp inputLsp = args.getLsp();
                Preconditions.checkArgument(inputLsp != null, "Reported LSP does not contain LSP object.");

                rb.fieldsFrom(input.getArguments());

                final TlvsBuilder tlvsBuilder;
                if (inputLsp.getTlvs() != null) {
                    tlvsBuilder = new TlvsBuilder(inputLsp.getTlvs());
                } else {
                    tlvsBuilder = new TlvsBuilder();
                }
                tlvsBuilder.setSymbolicPathName(
                    new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(input.getName().getBytes(Charsets.UTF_8))).build());

                rb.setSrp(new SrpBuilder().setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build());
                rb.setLsp(new LspBuilder().setAdministrative(inputLsp.isAdministrative()).setDelegate(inputLsp.isDelegate()).setPlspId(
                    new PlspId(0L)).setTlvs(tlvsBuilder.build()).build());

                final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
                ib.setRequests(Collections.singletonList(rb.build()));

                // Send the message
                return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId(),
                    input.getArguments().getMetadata());
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null, MISSING_XML_TAG);
        LOG.trace("RemoveLspArgs {}", input);
        // Make sure the LSP exists, we need it for PLSP-ID
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                if (!rep.isPresent()) {
                    LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
                    return OperationResults.UNSENT.future();
                }

                // it doesn't matter how many lsps there are in the path list, we only need delegate & plspid that is the same in each path
                final Path1 ra = rep.get().getPath().get(0).getAugmentation(Path1.class);
                Preconditions.checkState(ra != null, "Reported LSP reported null from data-store.");
                final Lsp reportedLsp = ra.getLsp();
                Preconditions.checkState(reportedLsp != null, "Reported LSP does not contain LSP object.");

                // Build the request and send it
                final RequestsBuilder rb = new RequestsBuilder();
                rb.setSrp(new SrpBuilder().addAugmentation(Srp1.class, new Srp1Builder().setRemove(Boolean.TRUE).build()).setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build());
                rb.setLsp(new LspBuilder().setRemove(Boolean.FALSE).setPlspId(reportedLsp.getPlspId()).setDelegate(reportedLsp.isDelegate()).build());

                final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
                ib.setRequests(Collections.singletonList(rb.build()));
                return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(), rb.getSrp().getOperationId(), null);
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        LOG.trace("UpdateLspArgs {}", input);
        // Make sure the LSP exists
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new AsyncFunction<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
                if (!rep.isPresent()) {
                    LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
                    return OperationResults.UNSENT.future();
                }

                // it doesn't matter how many lsps there are in the path list, we only need plspid that is the same in each path
                final Path1 ra = rep.get().getPath().get(0).getAugmentation(Path1.class);
                Preconditions.checkState(ra != null, "Reported LSP reported null from data-store.");
                final Lsp reportedLsp = ra.getLsp();
                Preconditions.checkState(reportedLsp != null, "Reported LSP does not contain LSP object.");

                // create mandatory objects
                final Srp srp = new SrpBuilder().setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE).build();

                final Lsp inputLsp = input.getArguments().getAugmentation(Arguments3.class).getLsp();
                Lsp lsp = null;
                if (inputLsp != null) {
                    lsp = new LspBuilder().setPlspId(reportedLsp.getPlspId()).setDelegate((inputLsp.isDelegate() != null) ? inputLsp.isDelegate() : false).setTlvs(inputLsp.getTlvs()).setAdministrative((inputLsp.isAdministrative() != null) ? inputLsp.isAdministrative() : false).build();
                } else {
                    lsp = new LspBuilder().setPlspId(reportedLsp.getPlspId()).build();
                }
                Message msg = null;
                // the D bit that was reported decides the type of PCE message sent
                Preconditions.checkNotNull(reportedLsp.isDelegate());
                if (reportedLsp.isDelegate()) {
                    // we already have delegation, send update
                    final UpdatesBuilder rb = new UpdatesBuilder();
                    rb.setSrp(srp);
                    rb.setLsp(lsp);
                    final PathBuilder pb = new PathBuilder();
                    pb.fieldsFrom(input.getArguments());
                    rb.setPath(pb.build());
                    final PcupdMessageBuilder ub = new PcupdMessageBuilder(MESSAGE_HEADER);
                    ub.setUpdates(Collections.singletonList(rb.build()));
                    msg = new PcupdBuilder().setPcupdMessage(ub.build()).build();
                } else {
                    // we want to revoke delegation, different type of message
                    // is sent because of specification by Siva
                    // this message is also sent, when input delegate bit is set to 0
                    // generating an error in PCC
                    final List<Requests> reqs = new ArrayList<>();
                    reqs.add(new RequestsBuilder().setSrp(srp).setLsp(lsp).build());
                    final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder();
                    ib.setRequests(reqs);
                    msg = new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build();
                }
                return sendMessage(msg, srp.getOperationId(), input.getArguments().getMetadata());
            }
        });
    }

    @Override
    public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
        Preconditions.checkArgument(input != null && input.getName() != null && input.getNode() != null && input.getArguments() != null, MISSING_XML_TAG);
        final OperationalStatus op;
        final Arguments1 aa = input.getArguments().getAugmentation(Arguments1.class);
        if (aa != null) {
            op = aa.getOperational();
        } else {
            op = null;
        }

        // Make sure the LSP exists
        final InstanceIdentifier<ReportedLsp> lsp = lspIdentifier(input.getName());
        LOG.debug("Checking if LSP {} has operational state {}", lsp, op);
        final ListenableFuture<Optional<ReportedLsp>> f = readOperationalData(lsp);

        return Futures.transform(f, new Function<Optional<ReportedLsp>, OperationResult>() {
            @Override
            public OperationResult apply(final Optional<ReportedLsp> rep) {
                if (!rep.isPresent()) {
                    LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
                    return OperationResults.UNSENT;
                }

                // check if at least one of the paths has the same status as requested
                boolean operational = false;
                for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.Path p : rep.get().getPath()) {
                    final Path1 p1 = p.getAugmentation(Path1.class);
                    if (p1 == null) {
                        LOG.warn("Node {} LSP {} does not contain data", input.getNode(), input.getName());
                        return OperationResults.UNSENT;
                    }
                    final Lsp l = p1.getLsp();
                    if (l.getOperational().equals(op)) {
                        operational = true;
                    }
                }

                return operational ? OperationResults.SUCCESS : OperationResults.UNSENT;
            }
        });
    }
}
