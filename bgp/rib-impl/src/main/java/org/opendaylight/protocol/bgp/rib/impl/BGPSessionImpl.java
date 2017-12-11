/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.nio.channels.NonWritableChannelException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.pojo.MultiPathSupportImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateImpl;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPSessionStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.AddPathCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public class BGPSessionImpl extends SimpleChannelInboundHandler<Notification> implements BGPSession,
        BGPSessionStateProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BGPSessionImpl.class);

    private static final Notification KEEP_ALIVE = new KeepaliveBuilder().build();

    private static final int KA_TO_DEADTIMER_RATIO = 3;

    private static final String EXTENDED_MSG_DECODER = "EXTENDED_MSG_DECODER";

    static final String END_OF_INPUT = "End of input detected. Close the session.";

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
    private final List<AddressFamilies> addPathTypes;
    private final int holdTimerValue;
    private final int keepAlive;
    private final AsNumber asNumber;
    private final Ipv4Address bgpId;
    private final BGPPeerRegistry peerRegistry;
    private final ChannelOutputLimiter limiter;
    private final BGPSessionStateImpl sessionState;
    private boolean terminationReasonNotified;

    public BGPSessionImpl(final BGPSessionListener listener, final Channel channel, final Open remoteOpen,
            final BGPSessionPreferences localPreferences, final BGPPeerRegistry peerRegistry) {
        this(listener, channel, remoteOpen, localPreferences.getHoldTime(), peerRegistry);
    }

    public BGPSessionImpl(final BGPSessionListener listener, final Channel channel, final Open remoteOpen,
            final int localHoldTimer, final BGPPeerRegistry peerRegistry) {
        this.listener = requireNonNull(listener);
        this.channel = requireNonNull(channel);
        this.limiter = new ChannelOutputLimiter(this);
        this.channel.pipeline().addLast(this.limiter);
        this.holdTimerValue = (remoteOpen.getHoldTimer() < localHoldTimer) ? remoteOpen.getHoldTimer() : localHoldTimer;
        LOG.info("BGP HoldTimer new value: {}", this.holdTimerValue);
        this.keepAlive = this.holdTimerValue / KA_TO_DEADTIMER_RATIO;
        this.asNumber = AsNumberUtil.advertizedAsNumber(remoteOpen);
        this.peerRegistry = peerRegistry;
        this.sessionState = new BGPSessionStateImpl();

        final Set<TablesKey> tts = Sets.newHashSet();
        final Set<BgpTableType> tats = Sets.newHashSet();
        final List<AddressFamilies> addPathCapabilitiesList = Lists.newArrayList();
        if (remoteOpen.getBgpParameters() != null) {
            for (final BgpParameters param : remoteOpen.getBgpParameters()) {
                for (final OptionalCapabilities optCapa : param.getOptionalCapabilities()) {
                    final CParameters cParam = optCapa.getCParameters();
                    if (cParam.getAugmentation(CParameters1.class) == null) {
                        continue;
                    }
                    if (cParam.getAugmentation(CParameters1.class).getMultiprotocolCapability() != null) {
                        final MultiprotocolCapability multi = cParam.getAugmentation(CParameters1.class).getMultiprotocolCapability();
                        final TablesKey tt = new TablesKey(multi.getAfi(), multi.getSafi());
                        LOG.trace("Added table type to sync {}", tt);
                        tts.add(tt);
                        tats.add(new BgpTableTypeImpl(tt.getAfi(), tt.getSafi()));
                    } else if (cParam.getAugmentation(CParameters1.class).getAddPathCapability() != null) {
                        final AddPathCapability addPathCap = cParam.getAugmentation(CParameters1.class).getAddPathCapability();
                        addPathCapabilitiesList.addAll(addPathCap.getAddressFamilies());
                    }
                }
            }
        }

        this.sync = new BGPSynchronization(this.listener, tts);
        this.tableTypes = tats;
        this.addPathTypes = addPathCapabilitiesList;

        if (!this.addPathTypes.isEmpty()) {
            final ChannelPipeline pipeline = this.channel.pipeline();
            final BGPByteToMessageDecoder decoder = pipeline.get(BGPByteToMessageDecoder.class);
            decoder.addDecoderConstraint(MultiPathSupport.class,
                    MultiPathSupportImpl.createParserMultiPathSupport(this.addPathTypes));
        }

        if (this.holdTimerValue != 0) {
            channel.eventLoop().schedule(this::handleHoldTimer, this.holdTimerValue, TimeUnit.SECONDS);
            channel.eventLoop().schedule(this::handleKeepaliveTimer, this.keepAlive, TimeUnit.SECONDS);
        }
        this.bgpId = remoteOpen.getBgpIdentifier();
        this.sessionState.advertizeCapabilities(this.holdTimerValue, channel.remoteAddress(), channel.localAddress(),
                this.tableTypes, remoteOpen.getBgpParameters());
    }

    /**
     * Set the extend message coder for current channel
     * The reason for separating this part from constructor is, in #channel.pipeline().replace(..), the
     * invokeChannelRead() will be invoked after the original message coder handler got removed. And there
     * is chance that before the session instance is fully initiated (constructor returns), a KeepAlive
     * message arrived already in the channel buffer. Thus #AbstractBGPSessionNegotiator.handleMessage(..)
     * gets invoked again and a deadlock is caused.  A BGP final state machine error will happen as BGP
     * negotiator is still in OPEN_SENT state as the session constructor hasn't returned yet.
     *
     * @param remoteOpen
     */
    public synchronized void setChannelExtMsgCoder(final Open remoteOpen) {
        final boolean enableExMess = BgpExtendedMessageUtil.advertizedBgpExtendedMessageCapability(remoteOpen);
        if (enableExMess) {
            this.channel.pipeline().replace(BGPMessageHeaderDecoder.class, EXTENDED_MSG_DECODER,
                    BGPMessageHeaderDecoder.getExtendedBGPMessageHeaderDecoder());
        }
    }

    @Override
    public synchronized void close() {
        if (this.state != State.IDLE) {
            if (!this.terminationReasonNotified) {
                this.writeAndFlush(new NotifyBuilder().setErrorCode(BGPError.CEASE.getCode())
                        .setErrorSubcode(BGPError.CEASE.getSubcode()).build());
            }
            this.closeWithoutMessage();
        }
    }

    /**
     * Handles incoming message based on their type.
     *
     * @param msg incoming message
     */
    synchronized void handleMessage(final Notification msg) {
        if (this.state == State.IDLE) {
            return;
        }
        try {
            // Update last reception time
            this.lastMessageReceivedAt = System.nanoTime();

            if (msg instanceof Open) {
                // Open messages should not be present here
                this.terminate(new BGPDocumentedException(null, BGPError.FSM_ERROR));
            } else if (msg instanceof Notify) {
                final Notify notify = (Notify) msg;
                // Notifications are handled internally
                LOG.info("Session closed because Notification message received: {} / {}, data={}",
                        notify.getErrorCode(),
                        notify.getErrorSubcode(),
                        notify.getData() != null ? ByteBufUtil.hexDump(notify.getData()) : null);
                notifyTerminationReasonAndCloseWithoutMessage(notify.getErrorCode(), notify.getErrorSubcode());
            } else if (msg instanceof Keepalive) {
                // Keepalives are handled internally
                LOG.trace("Received KeepAlive message.");
                this.kaCounter++;
                if (this.kaCounter >= 2) {
                    this.sync.kaReceived();
                }
            } else if (msg instanceof RouteRefresh) {
                this.listener.onMessage(this, msg);
            } else if (msg instanceof Update) {
                this.listener.onMessage(this, msg);
                this.sync.updReceived((Update) msg);
            } else {
                LOG.warn("Ignoring unhandled message: {}.", msg.getClass());
            }

            this.sessionState.messageReceived(msg);
        } catch (final BGPDocumentedException e) {
            this.terminate(e);
        }
    }

    private synchronized void notifyTerminationReasonAndCloseWithoutMessage(
            final Short errorCode,
            final Short errorSubcode) {
        this.terminationReasonNotified = true;
        this.closeWithoutMessage();
        this.listener.onSessionTerminated(this, new BGPTerminationReason(
                BGPError.forValue(errorCode, errorSubcode)));
    }

    synchronized void endOfInput() {
        if (this.state == State.UP) {
            LOG.info(END_OF_INPUT);
            this.listener.onSessionDown(this, new IOException(END_OF_INPUT));
        }
    }

    @GuardedBy("this")
    private ChannelFuture writeEpilogue(final ChannelFuture future, final Notification msg) {
        future.addListener(
                (ChannelFutureListener) f -> {
                    if (!f.isSuccess()) {
                        LOG.warn("Failed to send message {} to socket {}", msg, BGPSessionImpl.this.channel, f.cause());
                    } else {
                        LOG.trace("Message {} sent to socket {}", msg, BGPSessionImpl.this.channel);
                    }
                });
        this.lastMessageSentAt = System.nanoTime();
        this.sessionState.messageSent(msg);
        return future;
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

    synchronized ChannelFuture writeAndFlush(final Notification msg) {
        if (isWritable()) {
            return writeEpilogue(this.channel.writeAndFlush(msg), msg);
        }
        return this.channel.newFailedFuture(new NonWritableChannelException());
    }

    @VisibleForTesting
    synchronized void closeWithoutMessage() {
        if (this.state == State.IDLE) {
            return;
        }
        LOG.info("Closing session: {}", this);
        this.channel.close().addListener((ChannelFutureListener) future -> Preconditions.checkArgument(future.isSuccess(), "Channel failed to close: %s", future.cause()));
        removePeerSession();
        this.state = State.IDLE;
        this.sessionState.setSessionState(this.state);
    }

    /**
     * Closes BGP session from the parent with given reason. A message needs to be sent, but parent doesn't have to be
     * modified, because he initiated the closing. (To prevent concurrent modification exception).
     *
     * @param e BGPDocumentedException
     */
    private synchronized void terminate(final BGPDocumentedException e) {
        final BGPError error = e.getError();
        final byte[] data = e.getData();
        final NotifyBuilder builder = new NotifyBuilder().setErrorCode(error.getCode()).setErrorSubcode(error.getSubcode());
        if (data != null && data.length != 0) {
            builder.setData(data);
        }
        this.writeAndFlush(builder.build());
        notifyTerminationReasonAndCloseWithoutMessage(error.getCode(), error.getSubcode());
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
            this.terminate(new BGPDocumentedException(BGPError.HOLD_TIMER_EXPIRED));
        } else {
            this.channel.eventLoop().schedule(this::handleHoldTimer, nextHold - ct, TimeUnit.NANOSECONDS);
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
        }
        this.channel.eventLoop().schedule(this::handleKeepaliveTimer, nextKeepalive - ct, TimeUnit.NANOSECONDS);
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

    @Override
    public List<AddressFamilies> getAdvertisedAddPathTableTypes() {
        return this.addPathTypes;
    }

    @Override
    public List<BgpTableType> getAdvertisedGracefulRestartTableTypes() {
        return Collections.emptyList();
    }

    @VisibleForTesting
    synchronized void sessionUp() {
        this.state = State.UP;
        try {
            this.sessionState.setSessionState(this.state);
            this.listener.onSessionUp(this);
        } catch (final Exception e) {
            handleException(e);
            throw e;
        }
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

    private synchronized boolean isWritable() {
        return this.channel != null && this.channel.isWritable();
    }

    public ChannelOutputLimiter getLimiter() {
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

    @Override
    public synchronized void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        handleException(cause);
    }

    /**
     * Handle exception occurred in the PCEP session. The session in error state should be closed
     * properly so that it can be restored later.
     */
    @VisibleForTesting
    void handleException(final Throwable cause) {
        LOG.warn("BGP session {} encountered error", this, cause);
        if (cause.getCause() instanceof BGPDocumentedException) {
            this.terminate((BGPDocumentedException) cause.getCause());
        } else {
            this.terminate(new BGPDocumentedException(BGPError.CEASE));
        }
    }

    @Override
    public BGPSessionState getBGPSessionState() {
        return this.sessionState;
    }

    @Override
    public BGPTimersState getBGPTimersState() {
        return this.sessionState;
    }

    @Override
    public BGPTransportState getBGPTransportState() {
        return this.sessionState;
    }

    @Override
    public void registerMessagesCounter(final BGPMessagesListener bgpMessagesListener) {
        this.sessionState.registerMessagesCounter(bgpMessagesListener);
    }
}
