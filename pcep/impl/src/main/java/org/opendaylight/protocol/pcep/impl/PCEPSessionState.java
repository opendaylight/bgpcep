/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.util.StatisticsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.error.messages.LastReceivedErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.error.messages.LastSentErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;

final class PCEPSessionState {
    private final Open localOpen;
    private long sentMsgCount = 0;
    private long receivedMsgCount = 0;
    private long sentErrMsgCount = 0;
    private long receivedErrMsgCount = 0;
    private long lastSentMsgTimestamp = 0;
    private final PeerPref peerPref;
    private final LocalPref localPref;
    private final LastReceivedErrorBuilder lastReceivedErrorBuilder;
    private final LastSentErrorBuilder lastSentErrorBuilder;
    private final ErrorMessagesBuilder errorsBuilder;
    private final MessagesBuilder msgsBuilder;

    PCEPSessionState(final Open remoteOpen, final Open localOpen, final Channel channel) {
        requireNonNull(remoteOpen);
        requireNonNull(localOpen);
        requireNonNull(channel);
        this.localOpen = localOpen;
        this.peerPref = getRemotePref(remoteOpen, channel);
        this.localPref = getLocalPref(localOpen, channel);
        this.lastReceivedErrorBuilder = new LastReceivedErrorBuilder();
        this.lastSentErrorBuilder = new LastSentErrorBuilder();
        this.errorsBuilder = new ErrorMessagesBuilder();
        this.msgsBuilder = new MessagesBuilder();
    }

    Messages getMessages(final int unknownMessagesCount) {
        this.errorsBuilder.setReceivedErrorMsgCount(this.receivedErrMsgCount);
        this.errorsBuilder.setSentErrorMsgCount(this.sentErrMsgCount);
        this.errorsBuilder.setLastReceivedError(this.lastReceivedErrorBuilder.build());
        this.errorsBuilder.setLastSentError(this.lastSentErrorBuilder.build());
        this.msgsBuilder.setLastSentMsgTimestamp(this.lastSentMsgTimestamp);
        this.msgsBuilder.setReceivedMsgCount(this.receivedMsgCount);
        this.msgsBuilder.setSentMsgCount(this.sentMsgCount);
        this.msgsBuilder.setUnknownMsgReceived(unknownMessagesCount);
        this.msgsBuilder.setErrorMessages(this.errorsBuilder.build());
        return this.msgsBuilder.build();
    }

    public LocalPref getLocalPref() {
        return this.localPref;
    }

    PeerPref getPeerPref() {
        return this.peerPref;
    }

    void setLastSentError(final Message msg) {
        this.sentErrMsgCount++;
        final ErrorObject errObj = getErrorObject(msg);
        this.lastSentErrorBuilder.setErrorType(errObj.getType());
        this.lastSentErrorBuilder.setErrorValue(errObj.getValue());
    }

    void setLastReceivedError(final Message msg) {
        final ErrorObject errObj = getErrorObject(msg);
        this.receivedErrMsgCount++;
        this.lastReceivedErrorBuilder.setErrorType(errObj.getType());
        this.lastReceivedErrorBuilder.setErrorValue(errObj.getValue());
    }

    void updateLastReceivedMsg() {
        this.receivedMsgCount++;
    }

    void updateLastSentMsg() {
        this.lastSentMsgTimestamp = StatisticsUtil.getCurrentTimestampInSeconds();
        this.sentMsgCount++;
    }

    private static ErrorObject getErrorObject(final Message msg) {
        requireNonNull(msg);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message
                .PcerrMessage errMsg = ((PcerrMessage) msg).getPcerrMessage();
        return errMsg.getErrors().get(errMsg.getErrors().size() - 1).getErrorObject();
    }

    private static PeerPref getRemotePref(final Open open, final Channel channel) {
        final PeerPrefBuilder peerBuilder = new PeerPrefBuilder();
        peerBuilder.setDeadtimer(open.getDeadTimer());
        peerBuilder.setKeepalive(open.getKeepalive());
        peerBuilder.setIpAddress(((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress());
        peerBuilder.setSessionId(open.getSessionId().intValue());
        return peerBuilder.build();
    }

    private static LocalPref getLocalPref(final Open open, final Channel channel) {
        final LocalPrefBuilder peerBuilder = new LocalPrefBuilder();
        peerBuilder.setDeadtimer(open.getDeadTimer());
        peerBuilder.setKeepalive(open.getKeepalive());
        peerBuilder.setIpAddress(((InetSocketAddress) channel.localAddress()).getAddress().getHostAddress());
        peerBuilder.setSessionId(open.getSessionId().intValue());
        return peerBuilder.build();
    }

    public Open getLocalOpen() {
        return localOpen;
    }
}
