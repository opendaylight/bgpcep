/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Ticker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPSession. (Not final for testing.)
 */
@VisibleForTesting
public class PCEPSessionImpl extends SimpleChannelInboundHandler<Message> implements PCEPSession {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionImpl.class);
    private static final long MINUTE_NANOS = TimeUnit.MINUTES.toNanos(1);
    private static final Keepalive KEEPALIVE = new KeepaliveBuilder()
        .setKeepaliveMessage(new KeepaliveMessageBuilder().build())
        .build();

    /**
     * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
     */
    private volatile long lastMessageSentAt;

    /**
     * System.nanoTime value about when was received the last message.
     */
    private long lastMessageReceivedAt;

    private final Queue<Long> unknownMessagesTimes = new LinkedList<>();

    private final PCEPSessionListener listener;

    /**
     * Open Object with session characteristics that were accepted by another PCE (sent from this session).
     */
    private final Open localOpen;

    /**
     * Open Object with session characteristics for this session (sent from another PCE).
     */
    private final Open remoteOpen;

    private int maxUnknownMessages;

    // True if the listener should not be notified about events
    @GuardedBy("this")
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Channel channel;


    private final PCEPSessionState sessionState;

    private final Ticker ticker;

    PCEPSessionImpl(final PCEPSessionListener listener, final int maxUnknownMessages, final Channel channel,
            final Open localOpen, final Open remoteOpen) {
        this(listener, maxUnknownMessages, channel, localOpen, remoteOpen, Ticker.systemTicker());
    }

    @VisibleForTesting
    PCEPSessionImpl(final PCEPSessionListener listener, final int maxUnknownMessages, final Channel channel,
            final Open localOpen, final Open remoteOpen, final Ticker ticker) {
        this.listener = requireNonNull(listener);
        this.channel = requireNonNull(channel);
        this.localOpen = requireNonNull(localOpen);
        this.remoteOpen = requireNonNull(remoteOpen);
        this.ticker = requireNonNull(ticker);
        lastMessageReceivedAt = ticker.read();

        if (maxUnknownMessages != 0) {
            this.maxUnknownMessages = maxUnknownMessages;
        }

        final var deadValue = getDeadTimerValue();
        if (deadValue != 0) {
            channel.eventLoop().schedule(this::handleDeadTimer, deadValue, TimeUnit.SECONDS);
        }
        final var keepAliveValue = getKeepAliveTimerValue();
        if (keepAliveValue != 0) {
            channel.eventLoop().schedule(this::handleKeepaliveTimer, keepAliveValue, TimeUnit.SECONDS);
        }

        LOG.info("Session {}[{}] <-> {}[{}] started",
            channel.localAddress(), localOpen.getSessionId(), channel.remoteAddress(), remoteOpen.getSessionId());
        sessionState = new PCEPSessionState(remoteOpen, localOpen, channel);
    }

    public final @NonNegative short getKeepAliveTimerValue() {
        return localOpen.getKeepalive().toJava();
    }

    public final @NonNegative short getDeadTimerValue() {
        return remoteOpen.getDeadTimer().toJava();
    }

    /**
     * If DeadTimer expires, the session ends. If a message (whichever) was received during this period, the DeadTimer
     * will be rescheduled by DEAD_TIMER_VALUE + the time that has passed from the start of the DeadTimer to the time at
     * which the message was received. If the session was closed by the time this method starts to execute (the session
     * state will become IDLE), that rescheduling won't occur.
     */
    private synchronized void handleDeadTimer() {
        final long ct = ticker.read();

        final long nextDead = lastMessageReceivedAt + TimeUnit.SECONDS.toNanos(getDeadTimerValue());

        if (channel.isActive()) {
            if (ct >= nextDead) {
                LOG.debug("DeadTimer expired. {}", new Date());
                terminate(TerminationReason.EXP_DEADTIMER);
            } else {
                channel.eventLoop().schedule(this::handleDeadTimer, nextDead - ct, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * If KeepAlive Timer expires, sends KeepAlive message. If a message (whichever) was send during this period, the
     * KeepAlive Timer will be rescheduled by KEEP_ALIVE_TIMER_VALUE + the time that has passed from the start of the
     * KeepAlive timer to the time at which the message was sent. If the session was closed by the time this method
     * starts to execute (the session state will become IDLE), that rescheduling won't occur.
     */
    private void handleKeepaliveTimer() {
        final long ct = ticker.read();

        long nextKeepalive = lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());

        if (channel.isActive()) {
            if (ct >= nextKeepalive) {
                sendMessage(KEEPALIVE);
                nextKeepalive = lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());
            }

            channel.eventLoop().schedule(this::handleKeepaliveTimer, nextKeepalive - ct, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Handle exception occurred in the PCEP session. The session in error state should be closed
     * properly so that it can be restored later.
     */
    @VisibleForTesting
    void handleException(final Throwable cause) {
        LOG.error("Exception captured for session {}, closing session.", this, cause);
        terminate(TerminationReason.UNKNOWN);
    }

    /**
     * Sends message to serialization.
     *
     * @param msg to be sent
     */
    @Override
    public Future<Void> sendMessage(final Message msg) {
        final ChannelFuture f = channel.writeAndFlush(msg);
        lastMessageSentAt = ticker.read();
        sessionState.updateLastSentMsg();
        if (!(msg instanceof KeepaliveMessage)) {
            LOG.debug("PCEP Message enqueued: {}", msg);
        }
        if (msg instanceof PcerrMessage) {
            sessionState.setLastSentError(msg);
        }

        f.addListener((ChannelFutureListener) arg -> {
            if (arg.isSuccess()) {
                LOG.trace("Message sent to socket: {}", msg);
            } else {
                LOG.debug("Message not sent: {}", msg, arg.cause());
            }
        });

        return f;
    }

    @VisibleForTesting
    Future<Void> closeChannel() {
        LOG.info("Closing PCEP session channel: {}", channel);
        return channel.close();
    }

    @VisibleForTesting
    public synchronized boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes PCEP session without sending a Close message, as the channel is no longer active.
     */
    @Override
    public synchronized void close() {
        close(null);
    }

    /**
     * Closes PCEP session, cancels all timers, returns to state Idle, sends the Close Message. KeepAlive and DeadTimer
     * are cancelled if the state of the session changes to IDLE. This method is used to close the PCEP session from
     * inside the session or from the listener, therefore the parent of this session should be informed.
     */
    @Override
    public void close(final TerminationReason reason) {
        if (closed.getAndSet(true)) {
            LOG.debug("Session is already closed.");
            return;
        }
        // only send close message when the reason is provided
        if (reason != null) {
            LOG.info("Closing PCEP session with reason {}: {}", reason, this);
            sendMessage(new CloseBuilder().setCCloseMessage(
                    new CCloseMessageBuilder().setCClose(new CCloseBuilder()
                        .setReason(reason.getUintValue()).build()).build()).build());
        } else {
            LOG.info("Closing PCEP session: {}", this);
        }
        closeChannel();
    }

    @Override
    public Tlvs getRemoteTlvs() {
        return remoteOpen.getTlvs();
    }

    @Override
    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

    private synchronized void terminate(final TerminationReason reason) {
        if (closed.get()) {
            LOG.debug("Session {} is already closed.", this);
            return;
        }
        close(reason);
        listener.onSessionTerminated(this, new PCEPCloseTermination(reason));
    }

    synchronized void endOfInput() {
        if (!closed.getAndSet(true)) {
            listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
        }
    }

    private void sendErrorMessage(final PCEPErrors value) {
        this.sendErrorMessage(value, null);
    }

    /**
     * Sends PCEP Error Message with one PCEPError and Open Object.
     *
     * @param value PCEP errors value
     * @param open Open Object
     */
    private void sendErrorMessage(final PCEPErrors value, final Open open) {
        sendMessage(Util.createErrorMessage(value, open));
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
    void handleMalformedMessage(final PCEPErrors error) {
        final long ct = ticker.read();
        this.sendErrorMessage(error);
        if (error == PCEPErrors.CAPABILITY_NOT_SUPPORTED) {
            unknownMessagesTimes.add(ct);
            while (ct - unknownMessagesTimes.peek() > MINUTE_NANOS) {
                unknownMessagesTimes.remove();
            }
            if (unknownMessagesTimes.size() > maxUnknownMessages) {
                terminate(TerminationReason.TOO_MANY_UNKNOWN_MSGS);
            }
        }
    }

    /**
     * Handles incoming message. If the session is up, it notifies the user. The user is notified about every message
     * except KeepAlive.
     *
     * @param msg incoming message
     */
    public synchronized void handleMessage(final Message msg) {
        if (closed.get()) {
            LOG.debug("PCEP Session {} is already closed, skip handling incoming message {}", this, msg);
            return;
        }
        // Update last reception time
        lastMessageReceivedAt = ticker.read();
        sessionState.updateLastReceivedMsg();
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
            close();
            listener.onSessionTerminated(this, new PCEPCloseTermination(TerminationReason
                    .forValue(((CloseMessage) msg).getCCloseMessage().getCClose().getReason())));
        } else {
            // This message needs to be handled by the user
            if (msg instanceof PcerrMessage) {
                sessionState.setLastReceivedError(msg);
            }
            listener.onMessage(this, msg);
        }
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
            .add("channel", channel)
            .add("localOpen", localOpen)
            .add("remoteOpen", remoteOpen)
            .toString();
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", justification = "Handle unexpected exceptions")
    void sessionUp() {
        try {
            listener.onSessionUp(this);
        } catch (final RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    @VisibleForTesting
    final Queue<Long> getUnknownMessagesTimes() {
        return unknownMessagesTimes;
    }

    @Override
    public Messages getMessages() {
        return sessionState.getMessages(unknownMessagesTimes.size());
    }

    @Override
    public LocalPref getLocalPref() {
        return sessionState.getLocalPref();
    }

    @Override
    public PeerPref getPeerPref() {
        return sessionState.getPeerPref();
    }

    @Override
    public Open getLocalOpen() {
        return sessionState.getLocalOpen();
    }

    @Override
    //similar to bgp/rib-impl/src/main/java/org/opendaylight/protocol/bgp/rib/impl/BGPSessionImpl.java
    public final synchronized void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        LOG.debug("Channel {} inactive.", ctx.channel());
        endOfInput();
        super.channelInactive(ctx);
    }

    @Override
    protected final synchronized void channelRead0(final ChannelHandlerContext ctx, final Message msg) {
        LOG.debug("Message was received: {} from {}", msg, channel.remoteAddress());
        handleMessage(msg);
    }

    @Override
    public final synchronized void handlerAdded(final ChannelHandlerContext ctx) {
        sessionUp();
    }

    @Override
    public synchronized void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        handleException(cause);
    }

    @Override
    public Tlvs getLocalTlvs() {
        return localOpen.getTlvs();
    }
}
