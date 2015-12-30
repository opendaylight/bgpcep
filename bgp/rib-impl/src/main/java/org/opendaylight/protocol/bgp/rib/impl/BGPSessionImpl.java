/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStatistics;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public class BGPSessionImpl extends SimpleChannelInboundHandler<Notification> implements BGPSession, BGPSessionStatistics, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPSessionImpl.class);

    private static final Notification KEEP_ALIVE = new KeepaliveBuilder().build();

    private static final int KA_TO_DEADTIMER_RATIO = 3;

    /**
     * Internal session state.
     */
    public enum State {
        /**
         * The session object is created by the negotiator in OpenConfirm state. While in this state, the session object
         * is half-alive, e.g. the timers are running, but the session is not completely up, e.g. it has not been
         * announced to the listener. If the session is torn down in this state, we do not inform the listener.
         */
        OPEN_CONFIRM,
        /**
         * The session has been completely established.
         */
        UP,
        /**
         * The session has been closed. It will not be resurrected.
         */
        IDLE,
    }

    /**
     * System.nanoTime value about when was sent the last message.
     */
    @VisibleForTesting
    private long lastMessageSentAt;

    /**
     * System.nanoTime value about when was received the last message
     */
    private long lastMessageReceivedAt;

    private final BGPSessionListener listener;

    private final BGPSynchronization sync;

    private int kaCounter = 0;

    private final Channel channel;

    @GuardedBy("this")
    private State state = State.OPEN_CONFIRM;

    private final Set<BgpTableType> tableTypes;
    private final int holdTimerValue;
    private final int keepAlive;
    private final AsNumber asNumber;
    private final Ipv4Address bgpId;
    private final BGPPeerRegistry peerRegistry;
    private final ChannelOutputLimiter limiter;

    private BGPSessionStats sessionStats;

    public BGPSessionImpl(final BGPSessionListener listener, final Channel channel, final Open remoteOpen, final BGPSessionPreferences localPreferences,
            final BGPPeerRegistry peerRegistry) {
        this(listener, channel, remoteOpen, localPreferences.getHoldTime(), peerRegistry);
        this.sessionStats = new BGPSessionStats(remoteOpen, this.holdTimerValue, this.keepAlive, channel, Optional.of(localPreferences), this.tableTypes);
    }

    public BGPSessionImpl(final BGPSessionListener listener, final Channel channel, final Open remoteOpen, final int localHoldTimer,
            final BGPPeerRegistry peerRegistry) {
        this.listener = Preconditions.checkNotNull(listener);
        this.channel = Preconditions.checkNotNull(channel);
        this.limiter = new ChannelOutputLimiter(this);
        this.channel.pipeline().addLast(this.limiter);
        this.holdTimerValue = (remoteOpen.getHoldTimer() < localHoldTimer) ? remoteOpen.getHoldTimer() : localHoldTimer;
        LOG.info("BGP HoldTimer new value: {}", this.holdTimerValue);
        this.keepAlive = this.holdTimerValue / KA_TO_DEADTIMER_RATIO;
        this.asNumber = AsNumberUtil.advertizedAsNumber(remoteOpen);
        this.peerRegistry = peerRegistry;

        final Set<TablesKey> tts = Sets.newHashSet();
        final Set<BgpTableType> tats = Sets.newHashSet();
        if (remoteOpen.getBgpParameters() != null) {
            for (final BgpParameters param : remoteOpen.getBgpParameters()) {
                for (final OptionalCapabilities optCapa : param.getOptionalCapabilities()) {
                    final CParameters cParam = optCapa.getCParameters();
                    if ( cParam.getAugmentation(CParameters1.class) == null ||
                            cParam.getAugmentation(CParameters1.class).getMultiprotocolCapability() == null ) {
                        continue;
                    }
                    final MultiprotocolCapability multi = cParam.getAugmentation(CParameters1.class).getMultiprotocolCapability();
                    final TablesKey tt = new TablesKey(multi.getAfi(), multi.getSafi());
                    LOG.trace("Added table type to sync {}", tt);
                    tts.add(tt);
                    tats.add(new BgpTableTypeImpl(tt.getAfi(), tt.getSafi()));
                }
            }
        }

        this.sync = new BGPSynchronization(this.listener, tts);
        this.tableTypes = tats;

        if (this.holdTimerValue != 0) {
            channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleHoldTimer();
                }
            }, this.holdTimerValue, TimeUnit.SECONDS);

            channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleKeepaliveTimer();
                }
            }, this.keepAlive, TimeUnit.SECONDS);
        }
        this.bgpId = remoteOpen.getBgpIdentifier();
        this.sessionStats = new BGPSessionStats(remoteOpen, this.holdTimerValue, this.keepAlive, channel, Optional.<BGPSessionPreferences>absent(),
                this.tableTypes);
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing session: {}", this);

        if (this.state != State.IDLE) {
            this.writeAndFlush(new NotifyBuilder().setErrorCode(BGPError.CEASE.getCode()).setErrorSubcode(
                    BGPError.CEASE.getSubcode()).build());
            removePeerSession();
            this.channel.close();
            this.state = State.IDLE;
        }
    }

    /**
     * Handles incoming message based on their type.
     *
     * @param msg incoming message
     */
    public synchronized void handleMessage(final Notification msg) {
        // Update last reception time
        this.lastMessageReceivedAt = System.nanoTime();
        this.sessionStats.updateReceivedMsgTotal();

        if (msg instanceof Open) {
            // Open messages should not be present here
            this.terminate(BGPError.FSM_ERROR);
        } else if (msg instanceof Notify) {
            // Notifications are handled internally
            LOG.info("Session closed because Notification message received: {} / {}", ((Notify) msg).getErrorCode(),
                ((Notify) msg).getErrorSubcode());
            this.closeWithoutMessage();
            this.listener.onSessionTerminated(this, new BGPTerminationReason(BGPError.forValue(((Notify) msg).getErrorCode(),
                ((Notify) msg).getErrorSubcode())));
            this.sessionStats.updateReceivedMsgErr((Notify) msg);
        } else if (msg instanceof Keepalive) {
            // Keepalives are handled internally
            LOG.trace("Received KeepAlive messsage.");
            this.kaCounter++;
            this.sessionStats.updateReceivedMsgKA();
            if (this.kaCounter >= 2) {
                this.sync.kaReceived();
            }
        } else {
            // All others are passed up
            this.listener.onMessage(this, msg);
            this.sync.updReceived((Update) msg);
            this.sessionStats.updateReceivedMsgUpd();
        }
    }

    public synchronized void endOfInput() {
        if (this.state == State.UP) {
            this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
        }
    }

    @GuardedBy("this")
    private void writeEpilogue(final ChannelFuture future, final Notification msg) {
        future.addListener(
            new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture f) {
                    if (!f.isSuccess()) {
                        LOG.error("Failed to send message {} to socket {}", msg, f.cause(), BGPSessionImpl.this.channel);
                    } else {
                        LOG.info("Message {} sent to socket {}", msg, BGPSessionImpl.this.channel);
                    }
                }
            });
        this.lastMessageSentAt = System.nanoTime();
        this.sessionStats.updateSentMsgTotal();
        if (msg instanceof Update) {
            this.sessionStats.updateSentMsgUpd();
        } else if (msg instanceof Notify) {
            this.sessionStats.updateSentMsgErr((Notify) msg);
        }
    }

    void flush() {
        this.channel.flush();
    }

    synchronized void write(final Notification msg) {
        try {
            writeEpilogue(this.channel.write(msg), msg);
        } catch (final Exception e) {
            LOG.warn("Message {} was not sent.", msg, e);
        }
    }

    synchronized void writeAndFlush(final Notification msg) {
        writeEpilogue(this.channel.writeAndFlush(msg), msg);
    }

    private synchronized void closeWithoutMessage() {
        LOG.debug("Closing session: {}", this);
        removePeerSession();
        this.channel.close();
        this.state = State.IDLE;
    }

    /**
     * Closes PCEP session from the parent with given reason. A message needs to be sent, but parent doesn't have to be
     * modified, because he initiated the closing. (To prevent concurrent modification exception).
     *
     * @param error
     */
    private void terminate(final BGPError error) {
        this.writeAndFlush(new NotifyBuilder().setErrorCode(error.getCode()).setErrorSubcode(error.getSubcode()).build());
        this.closeWithoutMessage();

        this.listener.onSessionTerminated(this, new BGPTerminationReason(error));
    }

    private void removePeerSession() {
        if (this.peerRegistry != null) {
            this.peerRegistry.removePeerSession(StrictBGPPeerRegistry.getIpAddress(this.channel.remoteAddress()));
        }
    }

    /**
     * If HoldTimer expires, the session ends. If a message (whichever) was received during this period, the HoldTimer
     * will be rescheduled by HOLD_TIMER_VALUE + the time that has passed from the start of the HoldTimer to the time at
     * which the message was received. If the session was closed by the time this method starts to execute (the session
     * state will become IDLE), then rescheduling won't occur.
     */
    private synchronized void handleHoldTimer() {
        if (this.state == State.IDLE) {
            return;
        }

        final long ct = System.nanoTime();
        final long nextHold = this.lastMessageReceivedAt + TimeUnit.SECONDS.toNanos(this.holdTimerValue);

        if (ct >= nextHold) {
            LOG.debug("HoldTimer expired. {}", new Date());
            this.terminate(BGPError.HOLD_TIMER_EXPIRED);
        } else {
            this.channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    handleHoldTimer();
                }
            }, nextHold - ct, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * If KeepAlive Timer expires, sends KeepAlive message. If a message (whichever) was send during this period, the
     * KeepAlive Timer will be rescheduled by KEEP_ALIVE_TIMER_VALUE + the time that has passed from the start of the
     * KeepAlive timer to the time at which the message was sent. If the session was closed by the time this method
     * starts to execute (the session state will become IDLE), that rescheduling won't occur.
     */
    private synchronized void handleKeepaliveTimer() {
        if (this.state == State.IDLE) {
            return;
        }

        final long ct = System.nanoTime();
        long nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(this.keepAlive);

        if (ct >= nextKeepalive) {
            this.writeAndFlush(KEEP_ALIVE);
            nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(this.keepAlive);
            this.sessionStats.updateSentMsgKA();
        }
        this.channel.eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                handleKeepaliveTimer();
            }
        }, nextKeepalive - ct, TimeUnit.NANOSECONDS);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("channel", this.channel);
        toStringHelper.add("state", this.getState());
        return toStringHelper;
    }

    @Override
    public Set<BgpTableType> getAdvertisedTableTypes() {
        return this.tableTypes;
    }

    protected synchronized void sessionUp() {
        this.sessionStats.startSessionStopwatch();
        this.state = State.UP;
        this.listener.onSessionUp(this);
    }

    public synchronized State getState() {
        return this.state;
    }

    @Override
    public final Ipv4Address getBgpId() {
        return this.bgpId;
    }

    @Override
    public final AsNumber getAsNumber() {
        return this.asNumber;
    }

    synchronized boolean isWritable() {
        return this.channel != null && this.channel.isWritable();
    }

    void schedule(final Runnable task) {
        Preconditions.checkState(this.channel != null);
        this.channel.eventLoop().submit(task);
    }

    @VisibleForTesting
    protected synchronized void setLastMessageSentAt(final long lastMessageSentAt) {
        this.lastMessageSentAt = lastMessageSentAt;
    }

    @Override
    public synchronized BgpSessionState getBgpSesionState() {
        return this.sessionStats.getBgpSessionState(this.state);
    }

    @Override
    public synchronized void resetSessionStats() {
        this.sessionStats.resetStats();
    }

    ChannelOutputLimiter getLimiter() {
        return this.limiter;
    }

    @Override
    public final void channelInactive(final ChannelHandlerContext ctx) {
        LOG.debug("Channel {} inactive.", ctx.channel());
        this.endOfInput();

        try {
            super.channelInactive(ctx);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to delegate channel inactive event on channel " + ctx.channel(), e);
        }
    }

    @Override
    protected final void channelRead0(final ChannelHandlerContext ctx, final Notification msg) {
        LOG.debug("Message was received: {}", msg);
        this.handleMessage(msg);
    }

    @Override
    public final void handlerAdded(final ChannelHandlerContext ctx) {
        this.sessionUp();
    }
}
