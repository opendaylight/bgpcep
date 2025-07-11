/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.pcep.server.PathComputation;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.PathComputationClient1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Arguments3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Path1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcerr.pcerr.message.error.type.StatefulCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.MessageHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.ensure.lsp.operational.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.ReportedLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Non-final for testing
class PCEPTopologySessionListener extends AbstractTopologySessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologySessionListener.class);
    private static final PlspId PLSPID_ZERO = new PlspId(Uint32.ZERO);
    private static final SrpIdNumber SRPID_ZERO = new SrpIdNumber(Uint32.ZERO);
    private static final String MISSING_XML_TAG = "Mandatory XML tags are missing.";
    private static final MessageHeader MESSAGE_HEADER = new MessageHeader() {
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

    private final AtomicLong requestId = new AtomicLong(1L);

    @GuardedBy("this")
    private final List<PlspId> staleLsps = new ArrayList<>();

    private final PceServerProvider pceServerProvider;

    /**
     * Creates a new stateful topology session listener for given server session manager.
     */
    PCEPTopologySessionListener(final SessionStateRegistry stateRegistry,
            final ServerSessionManager serverSessionManager, final PceServerProvider pceServerProvider) {
        super(stateRegistry, serverSessionManager);
        // FIXME: requireNonNull(), except tests need to be updated
        this.pceServerProvider = pceServerProvider;
    }

    private static LspDbVersion geLspDbVersionTlv(final Lsp lsp) {
        final var tlvs = lsp.getTlvs();
        if (tlvs != null) {
            final var tlvs1 = tlvs.augmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .controller.pcep.sync.optimizations.rev200720.Tlvs1.class);
            if (tlvs1 != null) {
                return tlvs1.getLspDbVersion();
            }
        }
        return null;
    }

    @Override
    public synchronized ListenableFuture<OperationResult> triggerSync(final TriggerSyncArgs input) {
        if (isTriggeredInitialSynchro() && !isSynchronized()) {
            return triggerSynchronization(input);
        } else if (isSessionSynchronized() && isTriggeredReSyncEnabled()) {
            checkArgument(input != null && input.getNode() != null, MISSING_XML_TAG);
            return input.getName() == null ? triggerResyncronization(input) : triggerLspSyncronization(input);
        }
        return OperationResults.UNSENT.future();
    }

    private ListenableFuture<OperationResult> triggerLspSyncronization(final TriggerSyncArgs input) {
        LOG.trace("Trigger Lsp Resynchronization {}", input);

        // Make sure the LSP exists
        final var lsp = lspIdentifier(input.getName());
        final var f = readOperationalData(lsp);
        if (f == null) {
            return OperationResults.createUnsent(PCEPErrors.LSP_INTERNAL_ERROR).future();
        }
        return Futures.transformAsync(f, new ResyncLspFunction(input), MoreExecutors.directExecutor());
    }

    private ListenableFuture<OperationResult> triggerResyncronization(final TriggerSyncArgs input) {
        LOG.trace("Trigger Resynchronization {}", input);
        markAllLspAsStale();
        updatePccState(PccSyncState.PcepTriggeredResync);
        final PcupdMessageBuilder pcupdMessageBuilder = new PcupdMessageBuilder(MESSAGE_HEADER);
        final SrpIdNumber srpIdNumber = createUpdateMessageSync(pcupdMessageBuilder);
        final Message msg = new PcupdBuilder().setPcupdMessage(pcupdMessageBuilder.build()).build();
        return sendMessage(msg, srpIdNumber, null);
    }

    private ListenableFuture<OperationResult> triggerSynchronization(final TriggerSyncArgs input) {
        LOG.trace("Trigger Initial Synchronization {}", input);
        final PcupdMessageBuilder pcupdMessageBuilder = new PcupdMessageBuilder(MESSAGE_HEADER);
        final SrpIdNumber srpIdNumber = createUpdateMessageSync(pcupdMessageBuilder);
        final Message msg = new PcupdBuilder().setPcupdMessage(pcupdMessageBuilder.build()).build();
        return sendMessage(msg, srpIdNumber, null);
    }

    private SrpIdNumber createUpdateMessageSync(final PcupdMessageBuilder pcupdMessageBuilder) {
        // FIXME: not sure whether use 0 instead of nextRequest() or do not insert srp == SRP-ID-number = 0
        final var operationId = nextRequest();

        pcupdMessageBuilder.setUpdates(List.of(new UpdatesBuilder()
            // LSP mandatory in PCUpd
            .setLsp(new LspBuilder().setPlspId(PLSPID_ZERO).setSync(Boolean.TRUE).build())
            // SRP Mandatory in PCUpd
            .setSrp(new SrpBuilder().setOperationId(operationId).build())
            // ERO Mandatory in PCUpd
            .setPath(new PathBuilder().setEro(new EroBuilder().build()).build())
            .build()));

        return operationId;
    }

    @Holding("this")
    private void markAllLspAsStale() {
        staleLsps.addAll(lsps.keySet());
    }

    private boolean handleErrorMessage(final PcerrMessage message) {
        final var errMsg = message.getPcerrMessage();
        if (errMsg.getErrorType() instanceof StatefulCase) {
            final StatefulCase stat = (StatefulCase) errMsg.getErrorType();
            for (final Srps srps : stat.getStateful().nonnullSrps()) {
                final SrpIdNumber id = srps.getSrp().getOperationId();
                if (!SRPID_ZERO.equals(id)) {
                    final PCEPRequest req = removeRequest(id);
                    if (req != null) {
                        req.finish(OperationResults.createFailed(errMsg.getErrors()));
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

    private boolean isSolicited(final Srp srp, final Lsp lsp, final MessageContext ctx, final ReportedLspBuilder rlb) {
        if (srp == null) {
            return false;
        }
        final SrpIdNumber id = srp.getOperationId();
        if (SRPID_ZERO.equals(id)) {
            return false;
        }
        switch (lsp.getOperational()) {
            case Active:
            case Down:
            case Up:
                if (!isTriggeredSyncInProcess()) {
                    final PCEPRequest req = removeRequest(id);
                    if (req != null) {
                        LOG.debug("Request {} resulted in LSP operational state {}", id, lsp.getOperational());
                        rlb.setMetadata(req.getMetadata());
                        ctx.resolveRequest(req);
                    } else {
                        LOG.warn("Request ID {} not found in outstanding DB", id);
                    }
                }
                break;
            case GoingDown:
            case GoingUp:
                // These are transitive states, so we don't have to do anything, as they will be followed
                // up...
                break;
            default:
                break;
        }
        return true;
    }

    @Holding("this")
    private boolean manageNextReport(final Reports report, final MessageContext ctx) {
        final Lsp lsp = report.getLsp();
        final PlspId plspid = lsp.getPlspId();
        final Srp srp = report.getSrp();

        if (!lsp.getSync() && (plspid == null || PLSPID_ZERO.equals(plspid))) {
            purgeStaleLsps(ctx);
            if (isTriggeredSyncInProcess()) {
                if (srp == null) {
                    return false;
                }
                final SrpIdNumber id = srp.getOperationId();
                if (SRPID_ZERO.equals(id)) {
                    return false;
                }
                final PCEPRequest req = removeRequest(id);
                ctx.resolveRequest(req);
            }
            stateSynchronizationAchieved(ctx);
            return true;
        }
        final ReportedLspBuilder rlb = new ReportedLspBuilder();
        boolean solicited = false;
        solicited = isSolicited(srp, lsp, ctx, rlb);

        // if remove flag is set in SRP object, remove the tunnel immediately
        if (solicited) {
            final Srp1 initiatedSrp = srp.augmentation(Srp1.class);
            if (initiatedSrp != null && initiatedSrp.getRemove()) {
                super.removeLsp(ctx, plspid);
                return false;
            }
        }
        rlb.setPath(BindingMap.of(buildPath(report, srp, lsp)));
        String name = lookupLspName(plspid);
        if (lsp.getTlvs() != null && lsp.getTlvs().getSymbolicPathName() != null) {
            name = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(lsp.getTlvs().getSymbolicPathName().getPathName()
                    .getValue())).toString();
        }
        //get LspDB from LSP and write it to pcc's node
        final LspDbVersion lspDbVersion = geLspDbVersionTlv(lsp);
        if (lspDbVersion != null) {
            updatePccNode(ctx, new PathComputationClientBuilder()
                .addAugmentation(new PathComputationClient1Builder().setLspDbVersion(lspDbVersion).build()).build());
        }
        updateLsp(ctx, plspid, name, rlb, solicited, lsp.getRemove());
        unmarkStaleLsp(plspid);

        LOG.debug("LSP {} updated", lsp);
        return true;
    }

    private static Path buildPath(final Reports report, final Srp srp, final Lsp lsp) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client
                .attributes.path.computation.client.reported.lsp.PathBuilder pb = new org.opendaylight.yang.gen.v1
                .urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation
                .client.reported.lsp.PathBuilder();
        if (report.getPath() != null) {
            pb.fieldsFrom(report.getPath());
        }
        // LSP is mandatory (if there is none, parser will throw an exception)
        // this is to ensure a path will be created at any rate
        final Path1Builder p1Builder = new Path1Builder();
        p1Builder.setLsp(report.getLsp());
        final PathSetupType pst;
        if (srp != null && srp.getTlvs() != null && srp.getTlvs().getPathSetupType() != null) {
            pst = srp.getTlvs().getPathSetupType();
            p1Builder.setPathSetupType(pst);
        } else {
            pst = null;
        }
        pb.addAugmentation(p1Builder.build());
        final var tlvs = report.getLsp().getTlvs();
        if (tlvs != null) {
            if (tlvs.getLspIdentifiers() != null) {
                pb.setLspId(tlvs.getLspIdentifiers().getLspId());
            } else if (!PSTUtil.isDefaultPST(pst)) {
                pb.setLspId(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                        .LspId(lsp.getPlspId().getValue()));
            }
        }
        return pb.build();
    }

    private boolean handlePcreqMessage(final PcreqMessage message) {

        LOG.info("Start PcRequest Message handler");
        Message rep = null;

        /* Get a Path Computation to compute the Path from the Request */
        // TODO: Adjust Junit Test to avoid this test
        if (pceServerProvider == null) {
            rep = createErrorMsg(PCEPErrors.RESOURCE_LIMIT_EXCEEDED, Uint32.ZERO);
            sendMessage(rep, new SrpIdNumber(Uint32.ZERO), null);
            return false;
        }
        PathComputation pathComputation = pceServerProvider.getPathComputation();
        /* Reply with Error Message if no valid Path Computation is available */
        if (pathComputation == null) {
            rep = createErrorMsg(PCEPErrors.RESOURCE_LIMIT_EXCEEDED, Uint32.ZERO);
            sendMessage(rep, new SrpIdNumber(Uint32.ZERO), null);
            return false;
        }
        for (var req : message.nonnullRequests()) {
            LOG.debug("Process request {}", req);
            rep = pathComputation.computePath(req);
            SrpIdNumber repId = null;
            if (req.getRp() != null) {
                repId = new SrpIdNumber(req.getRp().getRequestId().getValue());
            } else {
                repId = new SrpIdNumber(Uint32.ZERO);
            }
            sendMessage(rep, repId, null);
        }
        return false;
    }

    @Override
    protected synchronized boolean onMessage(final MessageContext ctx, final Message message) {
        if (message instanceof PcerrMessage) {
            return handleErrorMessage((PcerrMessage) message);
        }
        if (message instanceof Pcreq) {
            LOG.info("PcReq detected. Start Request Message handler");
            return handlePcreqMessage(((Pcreq) message).getPcreqMessage());
        }
        if (!(message instanceof PcrptMessage)) {
            return true;
        }
        // FIXME: update just a field
        listenerState().updateLastReceivedRptMsg();
        final var rpt = ((PcrptMessage) message).getPcrptMessage();
        for (final Reports report : rpt.nonnullReports()) {
            if (!manageNextReport(report, ctx)) {
                return false;
            }
        }
        return false;
    }

    private SrpIdNumber nextRequest() {
        return new SrpIdNumber(Uint32.valueOf(requestId.getAndIncrement()));
    }

    @Override
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "SB does not grok TYPE_USE")
    public synchronized ListenableFuture<OperationResult> addLsp(final AddLspArgs input) {
        checkArgument(input != null && input.getName() != null && input.getNode() != null
                && input.getArguments() != null, MISSING_XML_TAG);
        LOG.trace("AddLspArgs {}", input);
        // Make sure there is no such LSP
        final var lsp = lspIdentifier(input.getName());
        final var f = readOperationalData(lsp);
        return f == null ? OperationResults.createUnsent(PCEPErrors.LSP_INTERNAL_ERROR).future()
                : Futures.transformAsync(f, new AddFunction(input, lsp), MoreExecutors.directExecutor());
    }

    @Override
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "SB does not grok TYPE_USE")
    public synchronized ListenableFuture<OperationResult> removeLsp(final RemoveLspArgs input) {
        checkArgument(input != null && input.getName() != null && input.getNode() != null, MISSING_XML_TAG);
        LOG.trace("RemoveLspArgs {}", input);
        // Make sure the LSP exists, we need it for PLSP-ID
        final var lsp = lspIdentifier(input.getName());
        final var f = readOperationalData(lsp);
        return f == null ? OperationResults.createUnsent(PCEPErrors.LSP_INTERNAL_ERROR).future()
                : Futures.transformAsync(f, rep -> {
                    final Lsp reportedLsp = validateReportedLsp(rep, input);
                    if (reportedLsp == null) {
                        return OperationResults.createUnsent(PCEPErrors.UNKNOWN_PLSP_ID).future();
                    }
                    final PcinitiateMessageBuilder ib = new PcinitiateMessageBuilder(MESSAGE_HEADER);
                    final Requests rb = buildRequest(rep, reportedLsp);
                    ib.setRequests(List.of(rb));
                    return sendMessage(new PcinitiateBuilder().setPcinitiateMessage(ib.build()).build(),
                        rb.getSrp().getOperationId(), null);
                }, MoreExecutors.directExecutor());
    }

    private Requests buildRequest(final Optional<ReportedLsp> rep, final Lsp reportedLsp) {
        // Build the request and send it
        final RequestsBuilder rb = new RequestsBuilder();
        final SrpBuilder srpBuilder = new SrpBuilder().addAugmentation(new Srp1Builder()
                .setRemove(Boolean.TRUE).build()).setOperationId(nextRequest()).setProcessingRule(Boolean.TRUE);
        getPST(rep).ifPresent(pst -> srpBuilder.setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                .rev250328.srp.object.srp.TlvsBuilder().setPathSetupType(pst).build()));
        rb.setSrp(srpBuilder.build());
        rb.setLsp(new LspBuilder().setRemove(Boolean.FALSE).setPlspId(reportedLsp.getPlspId())
                .setDelegate(reportedLsp.getDelegate()).build());
        return rb.build();
    }

    private ListenableFuture<OperationResult> redelegate(final Lsp reportedLsp, final Srp srp, final Lsp lsp,
            final UpdateLspArgs input) {
        // the D bit that was reported decides the type of PCE message sent
        final boolean isDelegate = requireNonNull(reportedLsp.getDelegate());
        final Message msg;
        if (isDelegate) {
            // we already have delegation, send update
            final UpdatesBuilder rb = new UpdatesBuilder();
            rb.setSrp(srp);
            rb.setLsp(lsp);
            if (input.getArguments().getAssociationGroup() != null) {
                rb.setAssociationGroup(input.getArguments().getAssociationGroup());
            }
            final PathBuilder pb = new PathBuilder();
            pb.fieldsFrom(input.getArguments());
            rb.setPath(pb.build());
            final PcupdMessageBuilder ub = new PcupdMessageBuilder(MESSAGE_HEADER);
            ub.setUpdates(List.of(rb.build()));
            msg = new PcupdBuilder().setPcupdMessage(ub.build()).build();
        } else {
            final Lsp1 lspCreateFlag = reportedLsp.augmentation(Lsp1.class);
            // we only retake delegation for PCE initiated tunnels
            if (lspCreateFlag != null && !lspCreateFlag.getCreate()) {
                LOG.warn("Unable to retake delegation of PCC-initiated tunnel: {}", reportedLsp);
                return OperationResults.createUnsent(PCEPErrors.UPDATE_REQ_FOR_NON_LSP).future();
            }
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

    @Override
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "SB does not grok TYPE_USE")
    public synchronized ListenableFuture<OperationResult> updateLsp(final UpdateLspArgs input) {
        checkArgument(input != null && input.getName() != null && input.getNode() != null
                && input.getArguments() != null, MISSING_XML_TAG);
        LOG.trace("UpdateLspArgs {}", input);
        // Make sure the LSP exists
        final var lsp = lspIdentifier(input.getName());
        final var f = readOperationalData(lsp);
        return f == null ? OperationResults.createUnsent(PCEPErrors.LSP_INTERNAL_ERROR).future()
                : Futures.transformAsync(f, new UpdateFunction(input), MoreExecutors.directExecutor());
    }

    @Override
    public synchronized ListenableFuture<OperationResult> ensureLspOperational(final EnsureLspOperationalInput input) {
        checkArgument(input != null && input.getName() != null && input.getNode() != null, MISSING_XML_TAG);
        final Arguments args = input.getArguments();
        checkArgument(args != null, MISSING_XML_TAG);

        final OperationalStatus op;
        final Arguments1 aa = args.augmentation(Arguments1.class);
        if (aa != null) {
            op = aa.getOperational();
        } else {
            op = null;
        }

        // Make sure the LSP exists
        final var lsp = lspIdentifier(input.getName());
        LOG.debug("Checking if LSP {} has operational state {}", lsp, op);
        final var f = readOperationalData(lsp);
        return f == null ? OperationResults.createUnsent(PCEPErrors.LSP_INTERNAL_ERROR).future()
                : listenableFuture(f, input, op);
    }

    private static ListenableFuture<OperationResult> listenableFuture(
            final ListenableFuture<Optional<ReportedLsp>> future, final EnsureLspOperationalInput input,
            final OperationalStatus op) {
        return Futures.transform(future, rep -> {
            if (rep.isEmpty()) {
                LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
                return OperationResults.UNSENT;
            }
            // check if at least one of the paths has the same status as requested
            for (var path : rep.orElseThrow().nonnullPath().values()) {
                final var lspPath = path.augmentation(Path1.class);
                if (lspPath == null) {
                    LOG.warn("Node {} LSP {} does not contain data", input.getNode(), input.getName());
                    return OperationResults.UNSENT;
                }
                if (op.equals(lspPath.getLsp().getOperational())) {
                    return OperationResults.SUCCESS;
                }
            }
            return OperationResults.UNSENT;
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected Lsp validateReportedLsp(final Optional<ReportedLsp> rep, final LspId input) {
        if (rep.isEmpty()) {
            LOG.debug("Node {} does not contain LSP {}", input.getNode(), input.getName());
            return null;
        }
        // it doesn't matter how many lsps there are in the path list, we only need data that is the same in each path
        final Path1 ra = rep.orElseThrow().getPath().values().iterator().next().augmentation(Path1.class);
        checkState(ra != null, "Reported LSP reported null from data-store.");
        final Lsp reportedLsp = ra.getLsp();
        checkState(reportedLsp != null, "Reported LSP does not contain LSP object.");
        return reportedLsp;
    }

    private static Optional<PathSetupType> getPST(final Optional<ReportedLsp> rep) {
        if (rep.isPresent()) {
            final Path1 path1 = rep.orElseThrow().getPath().values().iterator().next().augmentation(Path1.class);
            if (path1 != null) {
                final PathSetupType pst = path1.getPathSetupType();
                if (!PSTUtil.isDefaultPST(pst)) {
                    return Optional.of(pst);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Recover lspData and mark any LSPs in the LSP database that were previously reported by the PCC as stale.
     */
    @Override
    protected synchronized void loadLspData(final Node node, final Map<String, ReportedLsp> lspData,
            final Map<PlspId, String> lsps, final boolean incrementalSynchro) {
        //load node's lsps from DS
        final PathComputationClient pcc = node.augmentation(Node1.class).getPathComputationClient();
        for (final ReportedLsp reportedLsp : pcc.nonnullReportedLsp().values()) {
            final String lspName = reportedLsp.getName();
            lspData.put(lspName, reportedLsp);
            if (!reportedLsp.getPath().isEmpty()) {
                final Path1 path1 = reportedLsp.getPath().values().iterator().next().augmentation(Path1.class);
                if (path1 != null) {
                    final PlspId plspId = path1.getLsp().getPlspId();
                    if (!incrementalSynchro) {
                        staleLsps.add(plspId);
                    }
                    lsps.put(plspId, lspName);
                }
            }
        }
    }

    /**
     * When the PCC reports an LSP during state synchronization, if the LSP already
     * exists in the LSP database, the PCE MUST update the LSP database and
     * clear the stale marker from the LSP.
     *
     * @param plspId id
     */
    private synchronized void unmarkStaleLsp(final PlspId plspId) {
        staleLsps.remove(plspId);
    }

    /**
     * Purge any LSPs from the LSP database that are still marked as stale.
     *
     * @param ctx message context
     */
    private synchronized void purgeStaleLsps(final MessageContext ctx) {
        for (final PlspId plspId : staleLsps) {
            removeLsp(ctx, plspId);
        }
        staleLsps.clear();
    }

    private class ResyncLspFunction implements AsyncFunction<Optional<ReportedLsp>, OperationResult> {
        private final TriggerSyncArgs input;

        ResyncLspFunction(final TriggerSyncArgs input) {
            this.input = input;
        }

        @Override
        public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
            final Lsp reportedLsp = validateReportedLsp(rep, input);
            if (reportedLsp == null || rep.isEmpty()) {
                return OperationResults.createUnsent(PCEPErrors.UNKNOWN_PLSP_ID).future();
            }
            // mark lsp as stale
            final ReportedLsp staleLsp = rep.orElseThrow();
            if (!staleLsp.getPath().isEmpty()) {
                final Path1 path1 = staleLsp.getPath().values().iterator().next().augmentation(Path1.class);
                if (path1 != null) {
                    staleLsps.add(path1.getLsp().getPlspId());
                }
            }
            updatePccState(PccSyncState.PcepTriggeredResync);
            // create PCUpd with mandatory objects and LSP object set to 1
            final SrpBuilder srpBuilder = new SrpBuilder();
            srpBuilder.setOperationId(nextRequest());
            srpBuilder.setProcessingRule(Boolean.TRUE);

            getPST(rep).ifPresent(pst -> srpBuilder.setTlvs(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                    .rev250328.srp.object.srp.TlvsBuilder().setPathSetupType(pst).build()));

            final Srp srp = srpBuilder.build();
            final Lsp lsp = new LspBuilder().setPlspId(reportedLsp.getPlspId()).setSync(Boolean.TRUE).build();

            final Message msg = createPcepUpd(srp, lsp);
            return sendMessage(msg, srp.getOperationId(), null);
        }

        private Message createPcepUpd(final Srp srp, final Lsp lsp) {
            return new PcupdBuilder()
                .setPcupdMessage(new PcupdMessageBuilder(MESSAGE_HEADER)
                    .setUpdates(List.of(new UpdatesBuilder()
                        .setSrp(srp)
                        .setLsp(lsp)
                        .setPath(new PathBuilder().build())
                        .build()))
                    .build())
                .build();
        }
    }

    private class AddFunction implements AsyncFunction<Optional<ReportedLsp>, OperationResult> {
        private final AddLspArgs input;
        private final WithKey<ReportedLsp, ReportedLspKey> lsp;

        AddFunction(final AddLspArgs input, final WithKey<ReportedLsp, ReportedLspKey> lsp) {
            this.input = input;
            this.lsp = lsp;
        }

        @Override
        public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
            if (rep.isPresent()) {
                LOG.debug("Node {} already contains lsp {} at {}", input.getNode(), input.getName(), lsp);
                return OperationResults.createUnsent(PCEPErrors.USED_SYMBOLIC_PATH_NAME).future();
            }
            if (!isInitiationCapability()) {
                return OperationResults.createUnsent(PCEPErrors.CAPABILITY_NOT_SUPPORTED).future();
            }

            // Build the request
            final RequestsBuilder rb = new RequestsBuilder();
            final var args = input.getArguments();
            final Arguments2 args2 = args.augmentation(Arguments2.class);
            final Lsp inputLsp = args2 != null ? args2.getLsp() : null;
            if (inputLsp == null) {
                return OperationResults.createUnsent(PCEPErrors.LSP_MISSING).future();
            }

            rb.fieldsFrom(input.getArguments());

            boolean segmentRouting = !PSTUtil.isDefaultPST(args2.getPathSetupType());

            /* Call Path Computation if an ERO was not provided */
            if (rb.getEro() == null || rb.getEro().nonnullSubobject().isEmpty()) {
                /* Get a Path Computation to compute the Path from the Arguments */
                // TODO: Adjust Junit Test to avoid this test
                if (pceServerProvider == null) {
                    return OperationResults.createUnsent(PCEPErrors.ERO_MISSING).future();
                }
                PathComputation pathComputation = pceServerProvider.getPathComputation();
                if (pathComputation == null) {
                    return OperationResults.createUnsent(PCEPErrors.ERO_MISSING).future();
                }
                rb.setEro(pathComputation.computeEro(args.getEndpointsObj(), args.getBandwidth(),
                        args.getClassType(), args.getMetrics(), args.getXro(), args.getIro(), segmentRouting));
            }

            final TlvsBuilder tlvsBuilder;
            if (inputLsp.getTlvs() != null) {
                tlvsBuilder = new TlvsBuilder(inputLsp.getTlvs());
            } else {
                tlvsBuilder = new TlvsBuilder();
            }
            tlvsBuilder.setSymbolicPathName(
                    new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(input.getName()
                            .getBytes(StandardCharsets.UTF_8))).build());

            final SrpBuilder srpBuilder = new SrpBuilder()
                    .setOperationId(nextRequest())
                    .setProcessingRule(Boolean.TRUE);
            if (segmentRouting) {
                srpBuilder.setTlvs(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf
                                .stateful.rev250328.srp.object.srp.TlvsBuilder()
                                .setPathSetupType(args2.getPathSetupType()).build());
            }
            rb.setSrp(srpBuilder.build());
            if (args.getAssociationGroup() != null) {
                rb.setAssociationGroup(args.getAssociationGroup());
            }
            rb.setLsp(new LspBuilder()
                .setAdministrative(inputLsp.getAdministrative())
                .setDelegate(inputLsp.getDelegate())
                .setPlspId(PLSPID_ZERO)
                .setTlvs(tlvsBuilder.build())
                .build());

            // Send the message
            return sendMessage(new PcinitiateBuilder()
                .setPcinitiateMessage(new PcinitiateMessageBuilder(MESSAGE_HEADER)
                    .setRequests(List.of(rb.build()))
                    .build())
                .build(),
                rb.getSrp().getOperationId(), input.getArguments().getMetadata());
        }
    }

    private class UpdateFunction implements AsyncFunction<Optional<ReportedLsp>, OperationResult> {

        private final UpdateLspArgs input;

        UpdateFunction(final UpdateLspArgs input) {
            this.input = input;
        }

        @Override
        public ListenableFuture<OperationResult> apply(final Optional<ReportedLsp> rep) {
            final Lsp reportedLsp = validateReportedLsp(rep, input);
            if (reportedLsp == null) {
                return OperationResults.createUnsent(PCEPErrors.UNKNOWN_PLSP_ID).future();
            }
            // create mandatory objects
            final Arguments3 args = input.getArguments().augmentation(Arguments3.class);
            final SrpBuilder srpBuilder = new SrpBuilder();
            srpBuilder.setOperationId(nextRequest());
            srpBuilder.setProcessingRule(Boolean.TRUE);
            if (args != null && args.getPathSetupType() != null) {
                if (!PSTUtil.isDefaultPST(args.getPathSetupType())) {
                    srpBuilder.setTlvs(
                            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                                    .rev250328.srp.object.srp.TlvsBuilder()
                                    .setPathSetupType(args.getPathSetupType()).build());
                }
            } else {
                getPST(rep).ifPresent(pst -> srpBuilder.setTlvs(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                        .rev250328.srp.object.srp.TlvsBuilder().setPathSetupType(pst).build()));
            }
            final Srp srp = srpBuilder.build();
            final Lsp inputLsp = args != null ? args.getLsp() : null;
            final LspBuilder lspBuilder = new LspBuilder().setPlspId(reportedLsp.getPlspId());
            if (inputLsp == null) {
                return OperationResults.createUnsent(PCEPErrors.LSP_MISSING).future();
            }
            lspBuilder.setDelegate(Boolean.TRUE.equals(inputLsp.getDelegate()))
                    .setTlvs(inputLsp.getTlvs())
                    .setAdministrative(Boolean.TRUE.equals(inputLsp.getAdministrative()));
            return redelegate(reportedLsp, srp, lspBuilder.build(), input);
        }
    }

    private static Pcerr createErrorMsg(@NonNull final PCEPErrors pcepErrors, final Uint32 reqID) {
        return new PcerrBuilder()
            .setPcerrMessage(new PcerrMessageBuilder()
                .setErrorType(new RequestCaseBuilder()
                    .setRequest(new RequestBuilder()
                        .setRps(List.of(new RpsBuilder()
                            .setRp(new RpBuilder()
                                .setProcessingRule(false)
                                .setIgnore(false)
                                .setRequestId(new RequestId(reqID))
                                .build())
                            .build()))
                        .build())
                    .build())
                .setErrors(List.of(new ErrorsBuilder()
                    .setErrorObject(new ErrorObjectBuilder()
                        .setType(pcepErrors.getErrorType())
                        .setValue(pcepErrors.getErrorValue())
                        .build())
                    .build()))
                .build())
            .build();
    }
}
