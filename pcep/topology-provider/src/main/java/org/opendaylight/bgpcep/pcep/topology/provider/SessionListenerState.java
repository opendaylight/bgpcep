/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.config.yang.pcep.topology.provider.ErrorMessages;
import org.opendaylight.controller.config.yang.pcep.topology.provider.LastReceivedError;
import org.opendaylight.controller.config.yang.pcep.topology.provider.LastSentError;
import org.opendaylight.controller.config.yang.pcep.topology.provider.LocalPref;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Messages;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PeerCapabilities;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PeerPref;
import org.opendaylight.controller.config.yang.pcep.topology.provider.ReplyTime;
import org.opendaylight.controller.config.yang.pcep.topology.provider.SessionState;
import org.opendaylight.controller.config.yang.pcep.topology.provider.StatefulMessages;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

final class SessionListenerState {
    private long lastReceivedRptMsgTimestamp = 0;
    private long receivedRptMsgCount = 0;
    private long sentUpdMsgCount = 0;
    private long sentInitMsgCount = 0;
    private PeerCapabilities capa;
    private LocalPref localPref;
    private PeerPref peerPref;
    private final Stopwatch sessionUpDuration;

    private long minReplyTime = 0;
    private long maxReplyTime = 0;
    private long totalTime = 0;
    private long reqCount = 0;

    public SessionListenerState() {
        this.sessionUpDuration = Stopwatch.createUnstarted();
        this.capa = new PeerCapabilities();
    }

    public void init(final PCEPSession session) {
        Preconditions.checkNotNull(session);
        this.localPref = getLocalPref(session.getLocalPref());
        this.peerPref = getPeerPref(session.getPeerPref());
        resetSessionUpDuration();
        this.sessionUpDuration.start();
    }

    private void resetSessionUpDuration() {
        if (this.sessionUpDuration.isRunning()) {
            this.sessionUpDuration.reset();
        }
    }

    public void processRequestStats(final long duration) {
        if (this.minReplyTime == 0) {
            this.minReplyTime = duration;
        } else {
            if (duration < this.minReplyTime) {
                this.minReplyTime = duration;
            }
        }
        if (duration > this.maxReplyTime) {
            this.maxReplyTime = duration;
        }
        this.totalTime += duration;
        this.reqCount++;
    }

    public StatefulMessages getStatefulMessages() {
        final StatefulMessages msgs = new StatefulMessages();
        msgs.setLastReceivedRptMsgTimestamp(this.lastReceivedRptMsgTimestamp);
        msgs.setReceivedRptMsgCount(this.receivedRptMsgCount);
        msgs.setSentInitMsgCount(this.sentInitMsgCount);
        msgs.setSentUpdMsgCount(this.sentUpdMsgCount);
        return msgs;
    }

    public void resetStats(final PCEPSession session) {
        Preconditions.checkNotNull(session);
        this.receivedRptMsgCount = 0;
        this.sentInitMsgCount = 0;
        this.sentUpdMsgCount = 0;
        this.lastReceivedRptMsgTimestamp = 0;
        this.maxReplyTime = 0;
        this.minReplyTime = 0;
        this.totalTime = 0;
        this.reqCount = 0;
        session.resetStats();
    }

    public ReplyTime getReplyTime() {
        final ReplyTime time = new ReplyTime();
        long avg = 0;
        if (this.reqCount != 0) {
            avg = this.totalTime / this.reqCount;
        }
        time.setAverageTime(avg);
        time.setMaxTime(this.maxReplyTime);
        time.setMinTime(this.minReplyTime);
        return time;
    }

    public PeerCapabilities getPeerCapabilities() {
        return this.capa;
    }

    public SessionState getSessionState(final PCEPSession session) {
        Preconditions.checkNotNull(session);
        final SessionState state = new SessionState();
        state.setLocalPref(this.localPref);
        state.setPeerPref(this.peerPref);
        state.setMessages(getMessageStats(session.getMessages()));
        state.setSessionDuration(StatisticsUtil.formatElapsedTime(this.sessionUpDuration.elapsed(TimeUnit.SECONDS)));
        return state;
    }

    public void setPeerCapabilities(final PeerCapabilities capabilities) {
        this.capa = Preconditions.checkNotNull(capabilities);
    }

    public void updateLastReceivedRptMsg() {
        this.lastReceivedRptMsgTimestamp = StatisticsUtil.getCurrentTimestampInSeconds();
        this.receivedRptMsgCount++;
    }

    public void updateStatefulSentMsg(final Message msg) {
        if (msg instanceof Pcinitiate) {
            this.sentInitMsgCount++;
        } else if (msg instanceof Pcupd) {
            this.sentUpdMsgCount++;
        }
    }

    private static LocalPref getLocalPref(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.LocalPref localPref) {
        final LocalPref local = new LocalPref();
        local.setDeadtimer(localPref.getDeadtimer());
        local.setIpAddress(localPref.getIpAddress());
        local.setKeepalive(localPref.getKeepalive());
        local.setSessionId(localPref.getSessionId());
        return local;
    }

    private static PeerPref getPeerPref(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.PeerPref peerPref) {
        final PeerPref peer = new PeerPref();
        peer.setDeadtimer(peerPref.getDeadtimer());
        peer.setIpAddress(peerPref.getIpAddress());
        peer.setKeepalive(peerPref.getKeepalive());
        peer.setSessionId(peerPref.getSessionId());
        return peer;
    }

    private static Messages getMessageStats(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.Messages messages) {
        final LastReceivedError lastReceivedError = new LastReceivedError();
        lastReceivedError.setErrorType(messages.getErrorMessages().getLastReceivedError().getErrorType());
        lastReceivedError.setErrorValue(messages.getErrorMessages().getLastReceivedError().getErrorValue());
        final LastSentError lastSentError = new LastSentError();
        lastSentError.setErrorType(messages.getErrorMessages().getLastSentError().getErrorType());
        lastSentError.setErrorValue(messages.getErrorMessages().getLastSentError().getErrorValue());
        final ErrorMessages errMsgs = new ErrorMessages();
        errMsgs.setLastReceivedError(lastReceivedError);
        errMsgs.setLastSentError(lastSentError);
        errMsgs.setReceivedErrorMsgCount(messages.getErrorMessages().getReceivedErrorMsgCount());
        errMsgs.setSentErrorMsgCount(messages.getErrorMessages().getSentErrorMsgCount());
        final Messages msgs = new Messages();
        msgs.setErrorMessages(errMsgs);
        msgs.setLastSentMsgTimestamp(messages.getLastSentMsgTimestamp());
        msgs.setReceivedMsgCount(messages.getReceivedMsgCount());
        msgs.setSentMsgCount(messages.getSentMsgCount());
        msgs.setUnknownMsgReceived(msgs.getUnknownMsgReceived());
        return msgs;
    }

    public void stop() {
        resetSessionUpDuration();
    }
}
