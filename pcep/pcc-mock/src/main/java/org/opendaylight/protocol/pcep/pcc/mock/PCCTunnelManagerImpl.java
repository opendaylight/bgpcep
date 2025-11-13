/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLspTlvs;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLspTlvsEndofSync;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPcRtpMessage;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createSrp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.reqToRptPath;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.updToRptPath;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.pcep.pcc.mock.api.LspType;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public final class PCCTunnelManagerImpl implements PCCTunnelManager {
    private static final Optional<Srp> NO_SRP = Optional.empty();

    private final @GuardedBy("this") HashMap<Integer, PCCSession> sessions = new HashMap<>();
    private final @GuardedBy("this") HashMap<PlspId, PCCTunnel> tunnels = new HashMap<>();
    private final AtomicLong plspIDsCounter;
    private final String address;
    private final Timer timer;
    private final int redelegationTimeout;
    private final int stateTimeout;
    private final int lspsCount;
    private final Optional<TimerHandler> timerHandler;

    private PCCSyncOptimization syncOptimization;

    public PCCTunnelManagerImpl(final int lspsCount, final InetAddress address, final int redelegationTimeout,
                                final int stateTimeout, final Timer timer, final Optional<TimerHandler> timerHandler) {
        Preconditions.checkArgument(lspsCount >= 0);
        this.redelegationTimeout = redelegationTimeout;
        this.stateTimeout = stateTimeout;
        plspIDsCounter = new AtomicLong(lspsCount);
        this.address = InetAddresses.toAddrString(requireNonNull(address));
        this.timer = requireNonNull(timer);
        this.timerHandler = timerHandler;
        this.lspsCount = lspsCount;
    }

    protected void reportToAll(final Updates update, final PCCSession session) {
        final PlspId plspId = update.getLsp().getPlspId();
        final PCCTunnel tunnel = tunnels.get(plspId);
        final Uint32 srpId = update.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            if (hasDelegation(tunnel, session)) {
                final Srp srp = createSrp(srpId);
                final Path path = updToRptPath(update.getPath());
                final List<Subobject> subobjects = update.getPath().getEro().getSubobject();
                final Lsp lsp = update.getLsp();
                sendToAll(tunnel, plspId, subobjects, srp, path, lsp);
                //update tunnel state
                tunnel.setLspState(path);
            } else {
                session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, srpId));
            }
        } else {
            session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UNKNOWN_PLSP_ID, srpId));
        }
    }

    private void returnDelegation(final Updates update, final PCCSession session) {
        final PlspId plspId = update.getLsp().getPlspId();
        final PCCTunnel tunnel = tunnels.get(plspId);
        final Uint32 srpId = update.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if session really has a delegation
            if (hasDelegation(tunnel, session)) {
                //send report D=0
                final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.empty());
                final LspFlags lf = new LspFlagsBuilder(update.getLsp().getLspFlags()).setSync(true)
                    .setOperational(OperationalStatus.Up).setDelegate(false).build();
                final Pcrpt pcrtp = createPcRtpMessage(new LspBuilder(update.getLsp()).setLspFlags(lf)
                    .setTlvs(tlvs).build(), Optional.of(createSrp(srpId)), tunnel.getLspState());
                session.sendReport(pcrtp);
                //start state timer
                startStateTimeout(tunnel, plspId);
                //if PCC's LSP, start re-delegation timer
                if (tunnel.getType() == LspType.PCC_LSP) {
                    startRedelegationTimer(tunnel, plspId, session);
                } else {
                    //if PCE-initiated LSP, revoke delegation instantly
                    setDelegation(plspId, null);
                }
            } else {
                session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, srpId));
            }
        } else {
            session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UNKNOWN_PLSP_ID, srpId));
        }
    }

    protected void takeDelegation(final Requests request, final PCCSession session) {
        final PlspId plspId = request.getLsp().getPlspId();
        final PCCTunnel tunnel = tunnels.get(plspId);
        final Uint32 srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if tunnel has no delegation
            if (tunnel.getType() == LspType.PCE_LSP && (tunnel.getDelegationHolder() == -1
                    || tunnel.getDelegationHolder() == session.getId())) {
                //set delegation
                tunnel.cancelTimeouts();
                setDelegation(plspId, session);
                //send report
                final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.empty());
                final LspFlags lf = new LspFlagsBuilder(request.getLsp().getLspFlags()).setSync(true)
                    .setOperational(OperationalStatus.Up).setDelegate(true).build();
                session.sendReport(createPcRtpMessage(
                    new LspBuilder(request.getLsp()).setLspFlags(lf).setTlvs(tlvs).build(),
                    Optional.of(createSrp(srpId)), tunnel.getLspState()));
            } else {
                session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.LSP_NOT_PCE_INITIATED, srpId));
            }
        } else {
            session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UNKNOWN_PLSP_ID, srpId));
        }
    }

    @Override
    public synchronized void onSessionUp(final PCCSession session) {
        syncOptimization = new PCCSyncOptimization(session);
        lazyTunnelInicialization();

        //first session - delegate all PCC's LSPs only when reporting at startup
        if (!sessions.containsKey(session.getId()) && session.getId() == 0) {
            for (final PlspId plspId : tunnels.keySet()) {
                setDelegation(plspId, session);
            }
        }
        sessions.put(session.getId(), session);

        if (!syncOptimization.isTriggeredInitSyncEnabled()) {
            lspReport(session);
        }
    }

    @Override
    public synchronized void onSessionDown(final PCCSession session) {
        for (final Entry<PlspId, PCCTunnel> entry : tunnels.entrySet()) {
            final PCCTunnel tunnel = entry.getValue();
            final PlspId plspId = entry.getKey();
            //deal with delegations
            if (hasDelegation(tunnel, session)) {
                startStateTimeout(tunnel, entry.getKey());
                startRedelegationTimer(tunnel, plspId, session);
            }
        }
    }

    protected void addTunnel(final Requests request, final PCCSession session) {
        final PlspId plspId = new PlspId(Uint32.valueOf(plspIDsCounter.incrementAndGet()));
        final PCCTunnel tunnel = new PCCTunnel(request.getLsp().getTlvs().getSymbolicPathName()
                .getPathName().getValue(), session.getId(), LspType.PCE_LSP, reqToRptPath(request));
        final LspFlags lf = new LspFlagsBuilder(request.getLsp().getLspFlags()).setCreate(true).build();
        sendToAll(tunnel, plspId, request.getEro().getSubobject(),
                createSrp(request.getSrp().getOperationId().getValue()), tunnel.getLspState(),
                new LspBuilder(request.getLsp()).setLspFlags(lf).build());
        tunnels.put(plspId, tunnel);
    }

    protected void removeTunnel(final Requests request, final PCCSession session) {
        final PlspId plspId = request.getLsp().getPlspId();
        final PCCTunnel tunnel = tunnels.get(plspId);
        final Uint32 srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            if (tunnel.getType() == LspType.PCE_LSP) {
                if (hasDelegation(tunnel, session)) {
                    tunnels.remove(plspId);
                    sendToAll(tunnel, plspId, tunnel.getLspState().getEro().getSubobject(),
                        new SrpBuilder(request.getSrp()).setRemove(true).build(),
                        reqToRptPath(request), request.getLsp());
                } else {
                    session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UPDATE_REQ_FOR_NON_LSP, srpId));
                }
            } else {
                session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.LSP_NOT_PCE_INITIATED, srpId));
            }
        } else {
            session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UNKNOWN_PLSP_ID, srpId));
        }
    }

    @Override
    public void onMessagePcupd(final Updates update, final PCCSession session) {
        final Lsp lsp = update.getLsp();
        if (isInitialSyncTriggered(lsp)) {
            lspReport(session);
            timerHandler.ifPresent(TimerHandler::createDisconnectTask);
        } else if (isReSyncTriggered(lsp)) {
            handledDbTriggeredResync(update, session);
        } else if (Boolean.TRUE.equals(lsp.getLspFlags().getDelegate())) {
            //regular LSP update
            reportToAll(update, session);
        } else {
            //returning LSP delegation
            returnDelegation(update, session);
        }
    }

    @Override
    public void onMessagePcInitiate(final Requests request, final PCCSession session) {
        final Srp srp = request.getSrp();
        if (srp != null && srp.getRemove()) {
            //remove LSP
            removeTunnel(request, session);
        } else if (Boolean.TRUE.equals(request.getLsp().getLspFlags().getDelegate())
                && request.getEndpointsObj() == null) {
            //take LSP delegation
            takeDelegation(request, session);
        } else {
            //create LSP
            addTunnel(request, session);
        }
    }

    private Tlvs buildTlvs(final PCCTunnel tunnel, final Uint32 plspId,
            final Optional<List<Subobject>> subobjectsList) {
        final var subObject = subobjectsList.isPresent() ? subobjectsList.orElseThrow()
            : tunnel.getLspState().getEro().getSubobject();
        final String destinationAddress = getDestinationAddress(subObject, address);

        return createLspTlvs(plspId, true, destinationAddress, address, address,
                Optional.of(tunnel.getPathName()), syncOptimization.incrementLspDBVersion());
    }

    private synchronized void lazyTunnelInicialization() {
        if (tunnels.isEmpty()) {
            final Uint64 dbV = syncOptimization.getLocalLspDbVersionValue();
            if (dbV != null && syncOptimization.isSyncAvoidanceEnabled() && !dbV.equals(Uint64.ONE)) {
                tunnels.putAll(PCCTunnelBuilder.createTunnels(address, dbV.intValue()));
            } else {
                tunnels.putAll(PCCTunnelBuilder.createTunnels(address, lspsCount));
            }
        }
    }

    private boolean isReSyncTriggered(final Lsp lsp) {
        return syncOptimization.isTriggeredReSyncEnabled() && lsp.getLspFlags().getSync();
    }

    private boolean isInitialSyncTriggered(final Lsp lsp) {
        return lsp.getPlspId().getValue().toJava() == 0 && lsp.getLspFlags().getSync()
                && syncOptimization.isTriggeredInitSyncEnabled();
    }

    private void handledDbTriggeredResync(final Updates update, final PCCSession session) {
        syncOptimization.setResynchronizingState(true);
        final SrpIdNumber operationId = update.getSrp().getOperationId();
        if (update.getLsp().getPlspId().getValue().toJava() == 0) {
            reportAllKnownLsp(Optional.of(operationId), session);
        } else {
            reportLsp(update.getLsp().getPlspId(), operationId, session);
        }
        sendEndOfSynchronization(session, Optional.of(operationId));
        syncOptimization.setResynchronizingState(false);
    }

    private void lspReport(final PCCSession session) {
        if (!tunnels.isEmpty()) {
            if (!syncOptimization.isSyncAvoidanceEnabled()) {
                reportAllKnownLsp(session);
                sendEndOfSynchronization(session);
            } else if (!syncOptimization.doesLspDbMatch()) {
                if (syncOptimization.isDeltaSyncEnabled()) {
                    reportMissedLsp(session);
                    sendEndOfSynchronization(session);
                } else {
                    reportAllKnownLsp(session);
                    sendEndOfSynchronization(session);
                }
            }
        }
    }

    /**
     * Reports Missed Lsp when DbVersion doesnt match.
     */
    private void reportMissedLsp(final PCCSession session) {
        for (long missedLsp = syncOptimization.getRemoteLspDbVersionValue().longValue() + 1;
             missedLsp <= syncOptimization.getLocalLspDbVersionValue().longValue(); missedLsp++) {
            final Uint32 missed = Uint32.valueOf(missedLsp);
            final PlspId plspId = new PlspId(missed);
            final PCCTunnel tunnel = tunnels.get(plspId);
            createLspAndSendReport(missed, tunnel, session, Optional.empty(), NO_SRP);
        }
    }

    private void createLspAndSendReport(final Uint32 plspId, final PCCTunnel tunnel, final PCCSession session,
            final Optional<Boolean> isSync, final Optional<Srp> srp) {
        final boolean delegation = hasDelegation(tunnel, session);
        if (delegation) {
            tunnel.cancelTimeouts();
        }
        final String destinationAddress
                = getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), address);
        final Tlvs tlvs = createLspTlvs(plspId, true, destinationAddress, address,
                address, Optional.of(tunnel.getPathName()), syncOptimization.incrementLspDBVersion());

        final boolean sync = isSync.isPresent() ? isSync.orElseThrow() : syncOptimization.isSyncNeedIt();
        final Lsp lsp = createLsp(plspId, sync, Optional.ofNullable(tlvs), delegation, false);
        final Pcrpt pcrtp = createPcRtpMessage(lsp, srp, tunnel.getLspState());
        session.sendReport(pcrtp);
    }

    private void sendEndOfSynchronization(final PCCSession session) {
        sendEndOfSynchronization(session, Optional.empty());
    }

    private void sendEndOfSynchronization(final PCCSession session, final Optional<SrpIdNumber> operationId) {
        final var srp = operationId.map(id -> new SrpBuilder().setOperationId(id).build());
        final var tlv = syncOptimization.isSyncAvoidanceEnabled()
            ? createLspTlvsEndofSync(syncOptimization.incrementLspDBVersion().orElseThrow()) : Optional.<Tlvs>empty();
        final Pcrpt pcrtp = createPcRtpMessage(createLsp(Uint32.ZERO, false, tlv, true, false), srp,
            createPath(List.of()));
        session.sendReport(pcrtp);
    }

    private void reportAllKnownLsp(final PCCSession session) {
        reportAllKnownLsp(Optional.empty(), session);
    }

    private void reportAllKnownLsp(final Optional<SrpIdNumber> operationId, final PCCSession session) {
        final var srp = operationId.map(id -> new SrpBuilder().setOperationId(id).build());

        for (var entry : tunnels.entrySet()) {
            createLspAndSendReport(entry.getKey().getValue(), entry.getValue(), session, Optional.empty(), srp);
        }
    }

    private void reportLsp(final PlspId plspId, final SrpIdNumber operationId, final PCCSession session) {
        final PCCTunnel tunnel = tunnels.get(plspId);
        if (tunnel == null) {
            return;
        }
        final Srp srp = new SrpBuilder().setOperationId(operationId).build();
        createLspAndSendReport(plspId.getValue(), tunnel, session, Optional.of(Boolean.TRUE), Optional.of(srp));
    }

    private void sendToAll(final PCCTunnel tunnel, final PlspId plspId, final List<Subobject> subobjects, final Srp srp,
            final Path path, final Lsp lsp) {
        for (final PCCSession session : sessions.values()) {
            final boolean isDelegated = hasDelegation(tunnel, session);
            final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.of(subobjects));

            final Pcrpt pcRpt = createPcRtpMessage(
                new LspBuilder(lsp)
                    .setPlspId(plspId)
                    .setLspFlags(new LspFlagsBuilder(lsp.getLspFlags())
                        .setOperational(OperationalStatus.Up)
                        .setDelegate(isDelegated)
                        .setSync(true)
                        .setCreate(tunnel.getType() == LspType.PCE_LSP)
                        .build())
                    .setTlvs(tlvs)
                    .build(),
                Optional.ofNullable(srp), path);
            session.sendReport(pcRpt);
        }
    }

    private void startStateTimeout(final PCCTunnel tunnel, final PlspId plspId) {
        if (stateTimeout > -1) {
            final Timeout newStateTimeout = timer.newTimeout(timeout -> {
                if (tunnel.getType() == LspType.PCE_LSP) {
                    PCCTunnelManagerImpl.this.tunnels.remove(plspId);
                    //report tunnel removal to all
                    sendToAll(tunnel, plspId, List.of(), createSrp(Uint32.ZERO), new PathBuilder().build(),
                        createLsp(plspId.getValue(), false, Optional.empty(), false, true));
                }
            }, stateTimeout, TimeUnit.SECONDS);
            tunnel.setStateTimeout(newStateTimeout);
        }
    }

    private void startRedelegationTimer(final PCCTunnel tunnel, final PlspId plspId, final PCCSession session) {
        final Timeout newRedelegationTimeout = timer.newTimeout(timeout -> {
            //remove delegation
            PCCTunnelManagerImpl.this.setDelegation(plspId, null);
            //delegate to another PCE
            int index = session.getId();
            for (int i = 1; i < PCCTunnelManagerImpl.this.sessions.size(); i++) {
                index++;
                if (index == PCCTunnelManagerImpl.this.sessions.size()) {
                    index = 0;
                }
                final PCCSession nextSession = PCCTunnelManagerImpl.this.sessions.get(index);
                if (nextSession != null) {
                    tunnel.cancelTimeouts();
                    final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.empty());

                    nextSession.sendReport(createPcRtpMessage(
                        createLsp(plspId.getValue(), true, Optional.ofNullable(tlvs), true, false), NO_SRP,
                        tunnel.getLspState()));
                    tunnel.setDelegationHolder(nextSession.getId());
                    break;
                }
            }
        }, redelegationTimeout, TimeUnit.SECONDS);
        tunnel.setRedelegationTimeout(newRedelegationTimeout);
    }

    private void setDelegation(final PlspId plspId, final PCCSession session) {
        final PCCTunnel tunnel = tunnels.get(plspId);
        final int sessionId;
        if (session != null) {
            sessionId = session.getId();
        } else {
            sessionId = PCCTunnelBuilder.PCC_DELEGATION;
        }
        tunnel.setDelegationHolder(sessionId);
    }

    private static boolean hasDelegation(final PCCTunnel tunnel, final PCCSession session) {
        final int sessionId = session.getId();
        final int delegationHolder = tunnel.getDelegationHolder();
        return delegationHolder == sessionId;
    }

    private static String getDestinationAddress(final List<Subobject> subobjects, final String defaultAddress) {
        if (subobjects != null && !subobjects.isEmpty()) {
            final String prefix = ((IpPrefixCase) subobjects.get(subobjects.size() - 1).getSubobjectType())
                .getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
            return prefix.substring(0, prefix.indexOf('/'));
        }
        return defaultAddress;
    }
}
