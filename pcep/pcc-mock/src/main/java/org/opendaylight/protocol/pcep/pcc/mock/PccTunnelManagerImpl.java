/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLspTlvs;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createPcRtpMessage;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createSrp;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.reqToRptPath;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.updToRptPath;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.pcep.pcc.mock.api.PccSession;
import org.opendaylight.protocol.pcep.pcc.mock.api.PccTunnelManager;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

public class PccTunnelManagerImpl implements PccTunnelManager {

    private static final Subobject DEFAULT_ENDPOINT_HOP = getDefaultEROEndpointHop();
    private static final String ENDPOINT_ADDRESS = "1.1.1.1";
    private static final String ENDPOINT_PREFIX = ENDPOINT_ADDRESS + "/32";
    private static final Optional<Srp> NO_SRP = Optional.<Srp> absent();
    private static final int PCC_DELEGATION = -1;

    @GuardedBy("this")
    private final Map<Integer, PccSession> sessions = new HashMap<>();

    @GuardedBy("this")
    private final Map<PlspId, Tunnel> tunnels = new HashMap<>();

    private final AtomicLong plspIDsCounter;
    private final String address;
    private final Timer timer;
    private final int redelegationTimeout;
    private final int stateTimeout;

    public PccTunnelManagerImpl(final int lspsCount, final InetAddress address, final int redelegationTimeout,
            final int stateTimeout, final Timer timer) {
        Preconditions.checkArgument(lspsCount >= 0);
        Preconditions.checkNotNull(address);
        this.redelegationTimeout = redelegationTimeout;
        this.stateTimeout = stateTimeout;
        this.plspIDsCounter = new AtomicLong(lspsCount);
        this.address = InetAddresses.toAddrString(Preconditions.checkNotNull(address));
        this.timer = Preconditions.checkNotNull(timer);
        for (int i = 1; i <= lspsCount; i++) {
            final Tunnel tunnel = new Tunnel(MsgBuilderUtil.getDefaultPathName(this.address, i), PCC_DELEGATION, LspType.PCC_LSP,
                    createPath(Lists.newArrayList(DEFAULT_ENDPOINT_HOP)));
            tunnels.put(new PlspId((long) i), tunnel);
        }
    }

    @Override
    public void reportToAll(final Updates update, final PccSession session) {
        final PlspId plspId = update.getLsp().getPlspId();
        final Tunnel tunnel = tunnels.get(plspId);
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

    @Override
    public void returnDelegation(final Updates update, final PccSession session) {
        final PlspId plspId = update.getLsp().getPlspId();
        final Tunnel tunnel = tunnels.get(plspId);
        final long srpId = update.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if session really has a delegation
            if (hasDelegation(tunnel, session)) {
                //send report D=0
                final Tlvs tlvs = createLspTlvs(plspId.getValue(), true,
                        getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), this.address), this.address,
                        this.address, Optional.of(tunnel.getPathName()));
                session.sendReport(createPcRtpMessage(
                        new LspBuilder(update.getLsp()).setSync(true).setOperational(OperationalStatus.Up).setDelegate(false).setTlvs(tlvs).build(),
                        Optional.of(createSrp(srpId)), tunnel.getLspState()));
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

    @Override
    public synchronized void onSessionUp(final PccSession session) {
        //first session - delegate all PCC's LSPs
        //only when reporting at startup
        if (! sessions.containsKey(session.getId()) && session.getId() == 0) {
            for (final PlspId plspId : tunnels.keySet()) {
                setDelegation(plspId, session);
            }
        }
        sessions.put(session.getId(), session);
        if (!tunnels.isEmpty()) {
            //report all known LSPs
            for (final Entry<PlspId, Tunnel> entry : tunnels.entrySet()) {
                final Tunnel tunnel = entry.getValue();
                final boolean delegation = hasDelegation(tunnel, session);
                if (delegation) {
                    tunnel.cancelTimeouts();
                }
                final long plspId = entry.getKey().getValue();
                final Tlvs tlvs = MsgBuilderUtil.createLspTlvs(plspId, true,
                        getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), PccTunnelManagerImpl.this.address), this.address,
                        this.address, Optional.of(tunnel.getPathName()));
                session.sendReport(createPcRtpMessage(
                        createLsp(plspId, true, Optional.<Tlvs> fromNullable(tlvs), delegation, false), NO_SRP,
                        tunnel.getLspState()));
            }
            //end-of-sync marker
            session.sendReport(createPcRtpMessage(createLsp(0, false, Optional.<Tlvs> absent(), true, false), NO_SRP,
                    createPath(Collections.<Subobject> emptyList())));
        }
    }

    @Override
    public void takeDelegation(final Requests request, final PccSession session) {
        final PlspId plspId = request.getLsp().getPlspId();
        final Tunnel tunnel = tunnels.get(plspId);
        final long srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            //check if tunnel has no delegation
            if (tunnel.type == LspType.PCE_LSP && (tunnel.getDelegationHolder() == -1 || tunnel.getDelegationHolder() == session.getId())) {
                //set delegation
                tunnel.cancelTimeouts();
                setDelegation(plspId, session);
                //send report
                final Tlvs tlvs = createLspTlvs(plspId.getValue(), true,
                        getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), this.address), this.address,
                        this.address, Optional.of(tunnel.getPathName()));
                session.sendReport(createPcRtpMessage(
                        new LspBuilder(request.getLsp()).setSync(true).setOperational(OperationalStatus.Up).setDelegate(true).setTlvs(tlvs).build(),
                        Optional.of(createSrp(srpId)), tunnel.getLspState()));
            } else {
                session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.LSP_NOT_PCE_INITIATED, srpId));
            }
        } else {
            session.sendError(MsgBuilderUtil.createErrorMsg(PCEPErrors.UNKNOWN_PLSP_ID, srpId));
        }
    }

    @Override
    public synchronized void onSessionDown(final PccSession session) {
        for (final Entry<PlspId, Tunnel> entry : tunnels.entrySet()) {
            final Tunnel tunnel = entry.getValue();
            final PlspId plspId = entry.getKey();
            //deal with delegations
            if (hasDelegation(tunnel, session)) {
                startStateTimeout(tunnel, entry.getKey());
                startRedelegationTimer(tunnel, plspId, session);
            }
        }
    }

    @Override
    public void addTunnel(final Requests request, final PccSession session) {
        final PlspId plspId = new PlspId(this.plspIDsCounter.incrementAndGet());
        final Tunnel tunnel = new Tunnel(request.getLsp().getTlvs().getSymbolicPathName().getPathName().getValue(),
                session.getId(), LspType.PCE_LSP, reqToRptPath(request));
        sendToAll(tunnel, plspId, request.getEro().getSubobject(), createSrp(request.getSrp().getOperationId().getValue()),
                tunnel.getLspState(), new LspBuilder(request.getLsp()).addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(true).build()).build());
        tunnels.put(plspId, tunnel);
    }

    @Override
    public void removeTunnel(final Requests request, final PccSession session) {
        final PlspId plspId = request.getLsp().getPlspId();
        final Tunnel tunnel = tunnels.get(plspId);
        final long srpId = request.getSrp().getOperationId().getValue();
        if (tunnel != null) {
            if (tunnel.getType() == LspType.PCE_LSP) {
                if (hasDelegation(tunnel, session)) {
                    tunnels.remove(plspId);
                    sendToAll(tunnel, plspId, tunnel.getLspState().getEro().getSubobject(),
                            new SrpBuilder(request.getSrp()).addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build()).build(),
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

    private void sendToAll(final Tunnel tunnel, final PlspId plspId, final List<Subobject> subobjects, final Srp srp, final Path path, final Lsp lsp) {
        for (final PccSession session : sessions.values()) {
            final boolean isDelegated = hasDelegation(tunnel, session);
            final Tlvs tlvs = createLspTlvs(plspId.getValue(), true,
                    getDestinationAddress(subobjects, this.address), this.address,
                    this.address, Optional.of(tunnel.getPathName()));
            final Pcrpt pcRpt = createPcRtpMessage(
                    new LspBuilder(lsp).setPlspId(plspId).setOperational(OperationalStatus.Up).setDelegate(isDelegated).setTlvs(tlvs).build(),
                    Optional.fromNullable(srp), path);
            session.sendReport(pcRpt);
        }
    }

    private void startStateTimeout(final Tunnel tunnel, final PlspId plspId) {
        if (this.stateTimeout > -1) {
            final Timeout stateTimeout = this.timer.newTimeout(new TimerTask() {
                @Override
                public void run(final Timeout timeout) throws Exception {
                    if (tunnel.getType() == LspType.PCE_LSP) {
                        PccTunnelManagerImpl.this.tunnels.remove(plspId);
                        //report tunnel removal to all
                        sendToAll(tunnel, plspId, Collections.<Subobject>emptyList(), createSrp(0), new PathBuilder().build(),
                                createLsp(plspId.getValue(), false, Optional.<Tlvs>absent(), false, true));
                    }
                }
            }, this.stateTimeout, TimeUnit.SECONDS);
            tunnel.setStateTimeout(stateTimeout);
        }
    }

    private void startRedelegationTimer(final Tunnel tunnel, final PlspId plspId, final PccSession session) {
        final Timeout redelegationTimeout = this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) throws Exception {
                //remove delegation
                PccTunnelManagerImpl.this.setDelegation(plspId, null);
                //delegate to another PCE
                int index = session.getId();
                for (int i = 1; i < sessions.size(); i++) {
                    index++;
                    if (index == sessions.size()) {
                        index = 0;
                    }
                    final PccSession nextSession = sessions.get(index);
                    if (nextSession != null) {
                        tunnel.cancelTimeouts();
                        final Tlvs tlvs = createLspTlvs(plspId.getValue(), true,
                                getDestinationAddress(tunnel.getLspState().getEro().getSubobject(), PccTunnelManagerImpl.this.address),
                                PccTunnelManagerImpl.this.address, PccTunnelManagerImpl.this.address, Optional.of(tunnel.getPathName()));
                        nextSession.sendReport(createPcRtpMessage(
                                createLsp(plspId.getValue(), true, Optional.<Tlvs> fromNullable(tlvs), true, false), NO_SRP,
                                tunnel.getLspState()));
                        tunnel.setDelegationHolder(nextSession.getId());
                        break;
                    }
                }
            }
        }, this.redelegationTimeout, TimeUnit.SECONDS);
        tunnel.setRedelegationTimeout(redelegationTimeout);
    }

    private void setDelegation(final PlspId plspId, final PccSession session) {
        final Tunnel tunnel = tunnels.get(plspId);
        final int sessionId;
        if (session != null) {
            sessionId = session.getId();
        } else {
            sessionId = PCC_DELEGATION;
        }
        tunnel.setDelegationHolder(sessionId);
    }

    private static boolean hasDelegation(final Tunnel tunnel, final PccSession session) {
        return tunnel.getDelegationHolder() == session.getId();
    }

    private static final class Tunnel {
        private final byte[] pathName;
        private int delegationHolder;
        private final LspType type;
        private Path lspState;
        private Timeout redelegationTimeout;
        private Timeout stateTimeout;

        public Tunnel(final byte[] pathName, final int delegationHolder, final LspType type, final Path lspState) {
            this.pathName = pathName;
            this.delegationHolder = delegationHolder;
            this.type = type;
            this.lspState = lspState;
        }

        public byte[] getPathName() {
            return pathName;
        }

        public int getDelegationHolder() {
            return delegationHolder;
        }

        public LspType getType() {
            return type;
        }

        public Path getLspState() {
            return lspState;
        }

        public void setRedelegationTimeout(final Timeout redelegationTimeout) {
            this.redelegationTimeout = redelegationTimeout;
        }

        public void setStateTimeout(final Timeout stateTimeout) {
            this.stateTimeout = stateTimeout;
        }

        public void cancelTimeouts() {
            if (this.redelegationTimeout != null) {
                this.redelegationTimeout.cancel();
            }
            if (this.stateTimeout != null) {
                this.stateTimeout.cancel();
            }
        }

        public void setDelegationHolder(final int delegationHolder) {
            this.delegationHolder = delegationHolder;
        }

        public void setLspState(final Path lspState) {
            this.lspState = lspState;
        }

    }

    enum LspType {
        PCE_LSP, PCC_LSP
    }

    private static String getDestinationAddress(final List<Subobject> subobjects, final String defaultAddress) {
        if (subobjects != null && !subobjects.isEmpty()) {
            final String prefix = ((IpPrefixCase) subobjects.get(subobjects.size() - 1).getSubobjectType())
                    .getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
            return prefix.substring(0, prefix.indexOf('/'));
        }
        return defaultAddress;
    }

    private static Subobject getDefaultEROEndpointHop() {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(false);
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(new Ipv4Prefix(ENDPOINT_PREFIX))).build()).build());
        return builder.build();
    }

}
