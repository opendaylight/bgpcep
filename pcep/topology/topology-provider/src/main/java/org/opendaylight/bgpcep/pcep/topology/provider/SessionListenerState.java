/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

final class SessionListenerState {
    private final LongAdder lastReceivedRptMsgTimestamp = new LongAdder();
    private final LongAdder receivedRptMsgCount = new LongAdder();
    private final LongAdder sentUpdMsgCount = new LongAdder();
    private final LongAdder sentInitMsgCount = new LongAdder();
    private PeerCapabilities capa;
    private LocalPref localPref;
    private PeerPref peerPref;
    private final Stopwatch sessionUpDuration;

    private final LongAdder minReplyTime = new LongAdder();
    private final LongAdder maxReplyTime = new LongAdder();
    private final LongAdder totalTime = new LongAdder();
    private final LongAdder reqCount = new LongAdder();

    SessionListenerState() {
        this.sessionUpDuration = Stopwatch.createUnstarted();
        this.capa = new PeerCapabilities();
    }

    synchronized void init(final PCEPSession session) {
        requireNonNull(session);
        this.localPref = getLocalPref(session.getLocalPref());
        this.peerPref = getPeerPref(session.getPeerPref());
        this.sessionUpDuration.start();
    }

    synchronized void processRequestStats(final long duration) {
        if (this.minReplyTime.longValue() == 0) {
            this.minReplyTime.reset();
            this.minReplyTime.add(duration);
        } else if (duration < this.minReplyTime.longValue()) {
            this.minReplyTime.reset();
            this.minReplyTime.add(duration);
        }
        if (duration > this.maxReplyTime.longValue()) {
            this.maxReplyTime.reset();
            this.maxReplyTime.add(duration);
        }
        this.totalTime.add(duration);
        this.reqCount.increment();
    }

    synchronized StatefulMessages getStatefulMessages() {
        final StatefulMessages msgs = new StatefulMessages();
        msgs.setLastReceivedRptMsgTimestamp(this.lastReceivedRptMsgTimestamp.longValue());
        msgs.setReceivedRptMsgCount(this.receivedRptMsgCount.longValue());
        msgs.setSentInitMsgCount(this.sentInitMsgCount.longValue());
        msgs.setSentUpdMsgCount(this.sentUpdMsgCount.longValue());
        return msgs;
    }

    synchronized ReplyTime getReplyTime() {
        final ReplyTime time = new ReplyTime();
        long avg = 0;
        if (this.reqCount.longValue() != 0) {
            avg = Math.round((double)this.totalTime.longValue()/this.reqCount.longValue());
        }
        time.setAverageTime(avg);
        time.setMaxTime(this.maxReplyTime.longValue());
        time.setMinTime(this.minReplyTime.longValue());
        return time;
    }

    synchronized PeerCapabilities getPeerCapabilities() {
        return this.capa;
    }

    synchronized SessionState getSessionState(final PCEPSession session) {
        requireNonNull(session);
        final SessionState state = new SessionState();
        state.setLocalPref(this.localPref);
        state.setPeerPref(this.peerPref);
        state.setMessages(getMessageStats(session.getMessages()));
        state.setSessionDuration(StatisticsUtil.formatElapsedTime(this.sessionUpDuration.elapsed(TimeUnit.SECONDS)));
        return state;
    }

    synchronized void setPeerCapabilities(final PeerCapabilities capabilities) {
        this.capa = requireNonNull(capabilities);
    }

    synchronized void updateLastReceivedRptMsg() {
        this.lastReceivedRptMsgTimestamp.reset();
        this.lastReceivedRptMsgTimestamp.add(StatisticsUtil.getCurrentTimestampInSeconds());
        this.receivedRptMsgCount.increment();
    }

    synchronized void updateStatefulSentMsg(final Message msg) {
        if (msg instanceof Pcinitiate) {
            this.sentInitMsgCount.increment();
        } else if (msg instanceof Pcupd) {
            this.sentUpdMsgCount.increment();
        }
    }

    private static LocalPref getLocalPref(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .controller.pcep.stats.rev141006.pcep.session.state.LocalPref localPref) {
        final LocalPref local = new LocalPref();
        local.setDeadtimer(localPref.getDeadtimer());
        local.setIpAddress(localPref.getIpAddress());
        local.setKeepalive(localPref.getKeepalive());
        local.setSessionId(localPref.getSessionId());
        return local;
    }

    private static PeerPref getPeerPref(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .controller.pcep.stats.rev141006.pcep.session.state.PeerPref peerPref) {
        final PeerPref peer = new PeerPref();
        peer.setDeadtimer(peerPref.getDeadtimer());
        peer.setIpAddress(peerPref.getIpAddress());
        peer.setKeepalive(peerPref.getKeepalive());
        peer.setSessionId(peerPref.getSessionId());
        return peer;
    }

    private static Messages getMessageStats(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
        .controller.pcep.stats.rev141006.pcep.session.state.Messages messages) {
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
}
