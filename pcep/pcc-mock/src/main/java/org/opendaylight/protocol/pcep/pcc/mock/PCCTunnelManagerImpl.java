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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.pcep.pcc.mock.api.LspType;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;

public final class PCCTunnelManagerImpl implements PCCTunnelManager {

    private static final Optional<Srp> NO_SRP = Optional.absent();
    @GuardedBy("this")
    private final Map<Integer, PCCSession> sessions = new HashMap<>();
    private final AtomicLong plspIDsCounter;
    private final String address;
    private final Timer timer;
    private final int redelegationTimeout;
    private final int stateTimeout;
    private final int lspsCount;
    private final Optional<TimerHandler> timerHandler;
    @GuardedBy("this")
    private final Map<PlspId, PCCTunnel> tunnels = new HashMap<>();
    private PCCSyncOptimization syncOptimization;

    public PCCTunnelManagerImpl(final int lspsCount, final InetAddress address, final int redelegationTimeout,
                                final int stateTimeout, final Timer timer, final Optional<TimerHandler> timerHandler) {
        Preconditions.checkArgument(lspsCount >= 0);
        this.redelegationTimeout = redelegationTimeout;
        this.stateTimeout = stateTimeout;
        this.plspIDsCounter = new AtomicLong(lspsCount);
        this.address = InetAddresses.toAddrString(requireNonNull(address));
        this.timer = requireNonNull(timer);
        this.timerHandler = timerHandler;
        this.lspsCount = lspsCount;
    }

    protected void reportToAll(final Updates update, final PCCSession session) {
        final PlspId plspId = update.getLsp().getPlspId();
        final PCCTunnel tunnel = this.tunnels.get(plspId);
        final long srpId = update.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            if (hasDelegation(tunnel, session)) {
                final Srp srp = createSrp(update.getSrp().getOperationId().getValue());
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
        final PCCTunnel tunnel = this.tunnels.get(plspId);
        final long srpId = update.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if session really has a delegation
            if (hasDelegation(tunnel, session)) {
                //send report D=0
                final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.absent());
                final Pcrpt pcrtp = createPcRtpMessage(new LspBuilder(update.getLsp()).setSync(true)
                        .setOperational(OperationalStatus.Up).setDelegate(false)
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
        final PCCTunnel tunnel = this.tunnels.get(plspId);
        final long srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if tunnel has no delegation
            if ((tunnel.getType() == LspType.PCE_LSP) && ((tunnel.getDelegationHolder() == -1)
                    || (tunnel.getDelegationHolder() == session.getId()))) {
                //set delegation
                tunnel.cancelTimeouts();
                setDelegation(plspId, session);
                //send report
                final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.absent());
                session.sendReport(createPcRtpMessage(
                    new LspBuilder(request.getLsp()).setSync(true).setOperational(OperationalStatus.Up)
                            .setDelegate(true).setTlvs(tlvs).build(),
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
        this.syncOptimization = new PCCSyncOptimization(session);
        lazyTunnelInicialization();

        //first session - delegate all PCC's LSPs only when reporting at startup
        if (!this.sessions.containsKey(session.getId()) && (session.getId() == 0)) {
            for (final PlspId plspId : this.tunnels.keySet()) {
                setDelegation(plspId, session);
            }
        }
        this.sessions.put(session.getId(), session);

        if (!this.syncOptimization.isTriggeredInitSyncEnabled()) {
            lspReport(session);
        }
    }

    @Override
    public synchronized void onSessionDown(final PCCSession session) {
        for (final Entry<PlspId, PCCTunnel> entry : this.tunnels.entrySet()) {
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
        final PlspId plspId = new PlspId(this.plspIDsCounter.incrementAndGet());
        final PCCTunnel tunnel = new PCCTunnel(request.getLsp().getTlvs().getSymbolicPathName()
                .getPathName().getValue(), session.getId(), LspType.PCE_LSP, reqToRptPath(request));
        sendToAll(tunnel, plspId, request.getEro().getSubobject(),
                createSrp(request.getSrp().getOperationId().getValue()), tunnel.getLspState(),
                new LspBuilder(request.getLsp())
                        .addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(true).build()).build());
        this.tunnels.put(plspId, tunnel);
    }

    protected void removeTunnel(final Requests request, final PCCSession session) {
        final PlspId plspId = request.getLsp().getPlspId();
        final PCCTunnel tunnel = this.tunnels.get(plspId);
        final long srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            if (tunnel.getType() == LspType.PCE_LSP) {
                if (hasDelegation(tunnel, session)) {
                    this.tunnels.remove(plspId);
                    sendToAll(tunnel, plspId, tunnel.getLspState().getEro().getSubobject(),
                        new SrpBuilder(request.getSrp())
                                .addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build()).build(),
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
    public void onMessagePcupd(@Nonnull final Updates update, @Nonnull final PCCSession session) {
        final Lsp lsp = update.getLsp();
        if (isInitialSyncTriggered(lsp)) {
            lspReport(session);
            if (this.timerHandler.isPresent()) {
                this.timerHandler.get().createDisconnectTask();
            }
        } else if (isReSyncTriggered(lsp)) {
            handledDbTriggeredResync(update, session);
        } else if ((lsp.isDelegate() != null) && lsp.isDelegate()) {
            //regular LSP update
            reportToAll(update, session);
        } else {
            //returning LSP delegation
            returnDelegation(update, session);
        }
    }

    @Override
    public void onMessagePcInitiate(@Nonnull final Requests request, @Nonnull final PCCSession session) {
        if ((request.getSrp().getAugmentation(Srp1.class) != null)
                && request.getSrp().getAugmentation(Srp1.class).isRemove()) {
            //remove LSP
            removeTunnel(request, session);
        } else if ((request.getLsp().isDelegate() != null) && request.getLsp().isDelegate()
                && (request.getEndpointsObj() == null)) {
            //take LSP delegation
            takeDelegation(request, session);
        } else {
            //create LSP
            addTunnel(request, session);
        }
    }

    private Tlvs buildTlvs(final PCCTunnel tunnel, final Long plspId, final Optional<List<Subobject>> subobjectsList) {
        final List<Subobject> subObject = subobjectsList.isPresent() ? subobjectsList.get() :
                tunnel.getLspState().getEro().getSubobject();
        final String destinationAddress = getDestinationAddress(subObject, this.address);

        return createLspTlvs(plspId, true, destinationAddress, this.address, this.address,
                Optional.of(tunnel.getPathName()), this.syncOptimization.incrementLspDBVersion());
    }

    private synchronized void lazyTunnelInicialization() {
        if (this.tunnels.isEmpty()) {
            final BigInteger dbV = this.syncOptimization.getLocalLspDbVersionValue();
            if (dbV != null && this.syncOptimization.isSyncAvoidanceEnabled() && !dbV.equals(BigInteger.ONE)) {
                this.tunnels.putAll(PCCTunnelBuilder.createTunnels(this.address, dbV.intValue()));
            } else {
                this.tunnels.putAll(PCCTunnelBuilder.createTunnels(this.address, this.lspsCount));
            }
        }
    }

    private boolean isReSyncTriggered(final Lsp lsp) {
        return this.syncOptimization.isTriggeredReSyncEnabled() && lsp.isSync();
    }

    private boolean isInitialSyncTriggered(final Lsp lsp) {
        return (lsp.getPlspId().getValue() == 0) && lsp.isSync() && this.syncOptimization.isTriggeredInitSyncEnabled();
    }

    private void handledDbTriggeredResync(final Updates update, final PCCSession session) {
        this.syncOptimization.setResynchronizingState(Boolean.TRUE);
        final SrpIdNumber operationId = update.getSrp().getOperationId();
        if (update.getLsp().getPlspId().getValue() == 0) {
            reportAllKnownLsp(Optional.of(operationId), session);
        } else {
            reportLsp(update.getLsp().getPlspId(), operationId, session);
        }
        sendEndOfSynchronization(session, Optional.of(operationId));
        this.syncOptimization.setResynchronizingState(Boolean.FALSE);
    }

    private void lspReport(final PCCSession session) {
        if (!this.tunnels.isEmpty()) {
            if (!this.syncOptimization.isSyncAvoidanceEnabled()) {
                reportAllKnownLsp(session);
                sendEndOfSynchronization(session);
            } else if (!this.syncOptimization.doesLspDbMatch()) {
                if (this.syncOptimization.isDeltaSyncEnabled()) {
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
        for (long missedLsp = this.syncOptimization.getRemoteLspDbVersionValue().longValue() + 1;
             missedLsp <= this.syncOptimization.getLocalLspDbVersionValue().longValue(); missedLsp++) {
            final PlspId plspId = new PlspId(missedLsp);
            final PCCTunnel tunnel = this.tunnels.get(plspId);
            createLspAndSendReport(missedLsp, tunnel, session, Optional.absent(), NO_SRP);
        }
    }

    private void createLspAndSendReport(final long plspId, final PCCTunnel tunnel, final PCCSession session,
            final Optional<Boolean> isSync, final Optional<Srp> srp) {
        final boolean delegation = hasDelegation(tunnel, session);
        if (delegation) {
            tunnel.cancelTimeouts();
        }
        final String destinationAddress
                = getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), this.address);
        final Tlvs tlvs = createLspTlvs(plspId, true, destinationAddress, this.address,
                this.address, Optional.of(tunnel.getPathName()), this.syncOptimization.incrementLspDBVersion());

        final boolean sync = isSync.isPresent() ? isSync.get() : this.syncOptimization.isSyncNeedIt();
        final Lsp lsp = createLsp(plspId, sync, Optional.fromNullable(tlvs), delegation, false);
        final Pcrpt pcrtp = createPcRtpMessage(lsp, srp, tunnel.getLspState());
        session.sendReport(pcrtp);
    }

    private void sendEndOfSynchronization(final PCCSession session) {
        sendEndOfSynchronization(session, Optional.absent());
    }

    private void sendEndOfSynchronization(final PCCSession session, final Optional<SrpIdNumber> operationId) {
        Srp srp = null;
        if (operationId.isPresent()) {
            srp = new SrpBuilder().setOperationId(operationId.get()).build();
        }
        Optional<Tlvs> tlv = Optional.absent();
        if (this.syncOptimization.isSyncAvoidanceEnabled()) {
            tlv = createLspTlvsEndofSync(this.syncOptimization.incrementLspDBVersion().get());
        }
        final Pcrpt pcrtp = createPcRtpMessage(createLsp(0, false, tlv, true, false),
                Optional.fromNullable(srp), createPath(Collections.emptyList()));
        session.sendReport(pcrtp);
    }

    private void reportAllKnownLsp(final PCCSession session) {
        reportAllKnownLsp(Optional.absent(), session);
    }

    private void reportAllKnownLsp(final Optional<SrpIdNumber> operationId, final PCCSession session) {
        Srp srp = null;
        if (operationId.isPresent()) {
            srp = new SrpBuilder().setOperationId(operationId.get()).build();
        }

        for (final Entry<PlspId, PCCTunnel> entry : this.tunnels.entrySet()) {
            final PCCTunnel tunnel = entry.getValue();
            final long plspId = entry.getKey().getValue();
            createLspAndSendReport(plspId, tunnel, session, Optional.absent(), Optional.fromNullable(srp));
        }
    }

    private void reportLsp(final PlspId plspId, final SrpIdNumber operationId, final PCCSession session) {
        final PCCTunnel tunnel = this.tunnels.get(plspId);
        if (tunnel == null) {
            return;
        }
        final Srp srp = new SrpBuilder().setOperationId(operationId).build();
        createLspAndSendReport(plspId.getValue(), tunnel, session, Optional.of(Boolean.TRUE), Optional.of(srp));
    }

    private void sendToAll(final PCCTunnel tunnel, final PlspId plspId, final List<Subobject> subobjects, final Srp srp,
            final Path path, final Lsp lsp) {
        for (final PCCSession session : this.sessions.values()) {
            final boolean isDelegated = hasDelegation(tunnel, session);
            final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.of(subobjects));

            final Pcrpt pcRpt = createPcRtpMessage(
                new LspBuilder(lsp)
                    .setPlspId(plspId)
                    .setOperational(OperationalStatus.Up)
                    .setDelegate(isDelegated)
                    .setSync(true)
                    .addAugmentation(Lsp1.class, new Lsp1Builder()
                            .setCreate(tunnel.getType() == LspType.PCE_LSP).build())
                    .setTlvs(tlvs).build(),
                Optional.fromNullable(srp), path);
            session.sendReport(pcRpt);
        }
    }

    private void startStateTimeout(final PCCTunnel tunnel, final PlspId plspId) {
        if (this.stateTimeout > -1) {
            final Timeout newStateTimeout = this.timer.newTimeout(timeout -> {
                if (tunnel.getType() == LspType.PCE_LSP) {
                    PCCTunnelManagerImpl.this.tunnels.remove(plspId);
                    //report tunnel removal to all
                    sendToAll(tunnel, plspId, Collections.emptyList(), createSrp(0), new PathBuilder().build(),
                        createLsp(plspId.getValue(), false, Optional.absent(), false, true));
                }
            }, this.stateTimeout, TimeUnit.SECONDS);
            tunnel.setStateTimeout(newStateTimeout);
        }
    }

    private void startRedelegationTimer(final PCCTunnel tunnel, final PlspId plspId, final PCCSession session) {
        final Timeout newRedelegationTimeout = this.timer.newTimeout(timeout -> {
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
                    final Tlvs tlvs = buildTlvs(tunnel, plspId.getValue(), Optional.absent());

                    nextSession.sendReport(createPcRtpMessage(
                        createLsp(plspId.getValue(), true, Optional.fromNullable(tlvs), true, false), NO_SRP,
                        tunnel.getLspState()));
                    tunnel.setDelegationHolder(nextSession.getId());
                    break;
                }
            }
        }, this.redelegationTimeout, TimeUnit.SECONDS);
        tunnel.setRedelegationTimeout(newRedelegationTimeout);
    }

    private void setDelegation(final PlspId plspId, final PCCSession session) {
        final PCCTunnel tunnel = this.tunnels.get(plspId);
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
        if ((subobjects != null) && !subobjects.isEmpty()) {
            final String prefix = ((IpPrefixCase) subobjects.get(subobjects.size() - 1).getSubobjectType())
                .getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
            return prefix.substring(0, prefix.indexOf('/'));
        }
        return defaultAddress;
    }
}
