/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.PeerPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.messages.ErrorMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.messages.error.messages.LastReceivedErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.pcep.session.state.messages.error.messages.LastSentErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPSession. (Not final for testing.)
 */
@VisibleForTesting
public class PCEPSessionImpl extends AbstractProtocolSession<Message> implements PCEPSession {
    /**
     * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
     */
    @VisibleForTesting
    protected volatile long lastMessageSentAt;

    /**
     * System.nanoTime value about when was received the last message
     */
    private long lastMessageReceivedAt;

    private final Queue<Long> unknownMessagesTimes = new LinkedList<Long>();

    private final PCEPSessionListener listener;

    /**
     * Open Object with session characteristics that were accepted by another PCE (sent from this session).
     */
    private final Open localOpen;

    /**
     * Open Object with session characteristics for this session (sent from another PCE).
     */
    private final Open remoteOpen;

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionImpl.class);

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

    private int maxUnknownMessages;

    // True if the listener should not be notified about events
    private boolean closed = false;

    private final Channel channel;

    private final Keepalive kaMessage = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();

    PCEPSessionImpl(final PCEPSessionListener listener, final int maxUnknownMessages, final Channel channel,
        final Open localOpen, final Open remoteOpen) {
        this.listener = Preconditions.checkNotNull(listener);
        this.channel = Preconditions.checkNotNull(channel);
        this.localOpen = Preconditions.checkNotNull(localOpen);
        this.remoteOpen = Preconditions.checkNotNull(remoteOpen);
        this.lastMessageReceivedAt = System.nanoTime();

        if (maxUnknownMessages != 0) {
            this.maxUnknownMessages = maxUnknownMessages;
        }


        if (getDeadTimerValue() != 0) {
            channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleDeadTimer();
                }
            }, getDeadTimerValue(), TimeUnit.SECONDS);
        }

        if (getKeepAliveTimerValue() != 0) {
            channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleKeepaliveTimer();
                }
            }, getKeepAliveTimerValue(), TimeUnit.SECONDS);
        }

        LOG.info("Session {}[{}] <-> {}[{}] started", channel.localAddress(), localOpen.getSessionId(), channel.remoteAddress(),
            remoteOpen.getSessionId());

        final PeerPrefBuilder peerBuilder = new PeerPrefBuilder();
        peerBuilder.setDeadtimer(this.remoteOpen.getDeadTimer());
        peerBuilder.setKeepalive(this.remoteOpen.getKeepalive());
        peerBuilder.setIpAddress(((InetSocketAddress) this.channel.remoteAddress()).getAddress().getHostAddress());
        peerBuilder.setSessionId(this.remoteOpen.getSessionId().intValue());
        this.peerPref = peerBuilder.build();
        final LocalPrefBuilder localBuilder = new LocalPrefBuilder();
        localBuilder.setDeadtimer(this.localOpen.getDeadTimer());
        localBuilder.setKeepalive(this.localOpen.getKeepalive());
        localBuilder.setIpAddress(((InetSocketAddress)this.channel.localAddress()).getAddress().getHostAddress());
        localBuilder.setSessionId(this.localOpen.getSessionId().intValue());
        this.localPref = localBuilder.build();
        this.lastReceivedErrorBuilder = new LastReceivedErrorBuilder();
        this.lastSentErrorBuilder = new LastSentErrorBuilder();
        this.errorsBuilder = new ErrorMessagesBuilder();
        this.msgsBuilder = new MessagesBuilder();
    }

    public Integer getKeepAliveTimerValue() {
        return this.localOpen.getKeepalive().intValue();
    }

    public Integer getDeadTimerValue() {
        return this.remoteOpen.getDeadTimer().intValue();
    }

    /**
     * If DeadTimer expires, the session ends. If a message (whichever) was received during this period, the DeadTimer
     * will be rescheduled by DEAD_TIMER_VALUE + the time that has passed from the start of the DeadTimer to the time at
     * which the message was received. If the session was closed by the time this method starts to execute (the session
     * state will become IDLE), that rescheduling won't occur.
     */
    private synchronized void handleDeadTimer() {
        final long ct = System.nanoTime();

        final long nextDead = this.lastMessageReceivedAt + TimeUnit.SECONDS.toNanos(getDeadTimerValue());

        if (this.channel.isActive()) {
            if (ct >= nextDead) {
                LOG.debug("DeadTimer expired. {}", new Date());
                this.terminate(TerminationReason.ExpDeadtimer);
            } else {
                this.channel.eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                        handleDeadTimer();
                    }
                }, nextDead - ct, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * If KeepAlive Timer expires, sends KeepAlive message. If a message (whichever) was send during this period, the
     * KeepAlive Timer will be rescheduled by KEEP_ALIVE_TIMER_VALUE + the time that has passed from the start of the
     * KeepAlive timer to the time at which the message was sent. If the session was closed by the time this method
     * starts to execute (the session state will become IDLE), that rescheduling won't occur.
     */
    private  void handleKeepaliveTimer() {
        final long ct = System.nanoTime();

        long nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());

        if (this.channel.isActive()) {
            if (ct >= nextKeepalive) {
                this.sendMessage(this.kaMessage);
                nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());
            }

            this.channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleKeepaliveTimer();
                }
            }, nextKeepalive - ct, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Sends message to serialization.
     *
     * @param msg to be sent
     */
    @Override
    public Future<Void> sendMessage(final Message msg) {
        final ChannelFuture f = this.channel.writeAndFlush(msg);
        this.lastMessageSentAt = System.nanoTime();
        this.lastSentMsgTimestamp = System.currentTimeMillis();
        if (!(msg instanceof KeepaliveMessage)) {
            LOG.debug("PCEP Message enqueued: {}", msg);
        }
        if (msg instanceof PcerrMessage) {
            this.sentErrMsgCount++;
            final ErrorObject errObj = getErrorObject(msg);
            this.lastSentErrorBuilder.setErrorType(errObj.getType());
            this.lastSentErrorBuilder.setErrorValue(errObj.getValue());
        }
        this.sentMsgCount++;

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture arg) {
                if (arg.isSuccess()) {
                    LOG.trace("Message sent to socket: {}", msg);
                } else {
                    LOG.debug("Message not sent: {}", msg, arg.cause());
                }
            }
        });

        return f;
    }

    /**
     * Closes PCEP session without sending a Close message, as the channel is no longer active.
     */
    @Override
    public void close() {
        LOG.info("Closing PCEP session: {}", this);
        this.channel.close();
    }

    /**
     * Closes PCEP session, cancels all timers, returns to state Idle, sends the Close Message. KeepAlive and DeadTimer
     * are cancelled if the state of the session changes to IDLE. This method is used to close the PCEP session from
     * inside the session or from the listener, therefore the parent of this session should be informed.
     */
    @Override
    public synchronized void close(final TerminationReason reason) {
        LOG.info("Closing PCEP session: {}", this);
        this.closed = true;
        this.sendMessage(new CloseBuilder().setCCloseMessage(
            new CCloseMessageBuilder().setCClose(new CCloseBuilder().setReason(reason.getShortValue()).build()).build()).build());
        this.close();
    }

    @Override
    public Tlvs getRemoteTlvs() {
        return this.remoteOpen.getTlvs();
    }

    @Override
    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress) this.channel.remoteAddress()).getAddress();
    }

    private synchronized void terminate(final TerminationReason reason) {
        LOG.info("Local PCEP session termination : {}", reason);
        this.listener.onSessionTerminated(this, new PCEPCloseTermination(reason));
        this.closed = true;
        this.sendMessage(new CloseBuilder().setCCloseMessage(
            new CCloseMessageBuilder().setCClose(new CCloseBuilder().setReason(reason.getShortValue()).build()).build()).build());
        this.close();
    }

    @Override
    public synchronized void endOfInput() {
        if (!this.closed) {
            this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
            this.closed = true;
        }
    }

    private void sendErrorMessage(final PCEPErrors value) {
        this.sendErrorMessage(value, null);
    }

    /**
     * Sends PCEP Error Message with one PCEPError and Open Object.
     *
     * @param value
     * @param open
     */
    private void sendErrorMessage(final PCEPErrors value, final Open open) {
        this.sendMessage(Util.createErrorMessage(value, open));
    }

    /**
     * The fact, that a message is malformed, comes from parser. In case of unrecognized message a particular error is
     * sent (CAPABILITY_NOT_SUPPORTED) and the method checks if the MAX_UNKNOWN_MSG per minute wasn't overstepped.
     * Second, any other error occurred that is specified by rfc. In this case, the an error message is generated and
     * sent.
     *
     * @param error documented error in RFC5440 or draft
     */
    @VisibleForTesting
    public void handleMalformedMessage(final PCEPErrors error) {
        final long ct = System.nanoTime();
        this.sendErrorMessage(error);
        if (error == PCEPErrors.CAPABILITY_NOT_SUPPORTED) {
            this.unknownMessagesTimes.add(ct);
            while (ct - this.unknownMessagesTimes.peek() > 60 * 1E9) {
                this.unknownMessagesTimes.poll();
            }
            if (this.unknownMessagesTimes.size() > this.maxUnknownMessages) {
                this.terminate(TerminationReason.TooManyUnknownMsg);
            }
        }
    }

    /**
     * Handles incoming message. If the session is up, it notifies the user. The user is notified about every message
     * except KeepAlive.
     *
     * @param msg incoming message
     */
    @Override
    public synchronized void handleMessage(final Message msg) {
        // Update last reception time
        this.lastMessageReceivedAt = System.nanoTime();
        this.receivedMsgCount++;
        if (!(msg instanceof KeepaliveMessage)) {
            LOG.debug("PCEP message {} received.", msg);
        }
        // Internal message handling. The user does not see these messages
        if (msg instanceof KeepaliveMessage) {
            // Do nothing, the timer has been already reset
        } else if (msg instanceof OpenMessage) {
            this.sendErrorMessage(PCEPErrors.ATTEMPT_2ND_SESSION);
        } else if (msg instanceof CloseMessage) {
            /*
             * Session is up, we are reporting all messages to user. One notable
             * exception is CLOSE message, which needs to be converted into a
             * session DOWN event.
             */
            this.close();
        } else {
            // This message needs to be handled by the user
            if (msg instanceof PcerrMessage) {
                final ErrorObject errObj = getErrorObject(msg);
                this.receivedErrMsgCount++;
                this.lastReceivedErrorBuilder.setErrorType(errObj.getType());
                this.lastReceivedErrorBuilder.setErrorValue(errObj.getValue());
            }
            this.listener.onMessage(this, msg);
        }
    }

    @Override
    public final String toString() {
        return addToStringAttributes(Objects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("channel", this.channel);
        toStringHelper.add("localOpen", this.localOpen);
        toStringHelper.add("remoteOpen", this.remoteOpen);
        return toStringHelper;
    }

    @Override
    @VisibleForTesting
    public void sessionUp() {
        this.listener.onSessionUp(this);
    }

    @VisibleForTesting
    protected final Queue<Long> getUnknownMessagesTimes() {
        return this.unknownMessagesTimes;
    }

    @Override
    public Messages getMessages() {
        this.errorsBuilder.setReceivedErrorMsgCount(this.receivedErrMsgCount);
        this.errorsBuilder.setSentErrorMsgCount(this.sentErrMsgCount);
        this.errorsBuilder.setLastReceivedError(this.lastReceivedErrorBuilder.build());
        this.errorsBuilder.setLastSentError(this.lastSentErrorBuilder.build());
        this.msgsBuilder.setLastSentMsgTimestamp(TimeUnit.MILLISECONDS.toSeconds(this.lastSentMsgTimestamp));
        this.msgsBuilder.setReceivedMsgCount(this.receivedMsgCount);
        this.msgsBuilder.setSentMsgCount(this.sentMsgCount);
        this.msgsBuilder.setUnknownMsgReceived(this.unknownMessagesTimes.size());
        this.msgsBuilder.setErrorMessages(this.errorsBuilder.build());
        return this.msgsBuilder.build();
    }

    @Override
    public LocalPref getLocalPref() {
        return this.localPref;
    }

    @Override
    public PeerPref getPeerPref() {
        return this.peerPref;
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void resetStats() {
        this.receivedMsgCount = 0;
        this.sentMsgCount = 0;
        this.receivedErrMsgCount = 0;
        this.sentErrMsgCount = 0;
        this.lastSentMsgTimestamp = 0;
        this.lastReceivedErrorBuilder.setErrorType((short) 0);
        this.lastReceivedErrorBuilder.setErrorValue((short) 0);
        this.lastSentErrorBuilder.setErrorType((short) 0);
        this.lastSentErrorBuilder.setErrorValue((short) 0);
    }

    private static ErrorObject getErrorObject(final Message msg) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessage errMsg =
                ((PcerrMessage) msg).getPcerrMessage();
        return errMsg.getErrors().get(errMsg.getErrors().size() - 1).getErrorObject();
    }
}
