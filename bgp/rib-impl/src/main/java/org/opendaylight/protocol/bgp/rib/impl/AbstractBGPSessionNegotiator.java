/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.SessionNegotiator;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bgp Session negotiator. Common for local-to-remote and remote-to-local connections.
 * One difference is session validation performed by injected BGPSessionValidator when OPEN message is received.
 */
abstract class AbstractBGPSessionNegotiator extends ChannelInboundHandlerAdapter implements SessionNegotiator {
    // 4 minutes recommended in http://tools.ietf.org/html/rfc4271#section-8.2.2
    private static final int INITIAL_HOLDTIMER = 4;

    // <a href="http://tools.ietf.org/html/rfc6793">BGP Support for 4-Octet AS Number Space</a>
    @VisibleForTesting
    static final Uint16 AS_TRANS = Uint16.valueOf(23456).intern();
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBGPSessionNegotiator.class);
    private final BGPPeerRegistry registry;
    private final Promise<BGPSessionImpl> promise;
    private final Channel channel;
    @GuardedBy("this")
    private State state = State.IDLE;
    @GuardedBy("this")
    private BGPSessionImpl session;
    @GuardedBy("this")
    private ScheduledFuture<?> pending;

    @VisibleForTesting
    public enum State {
        /**
         * Negotiation has not started yet.
         */
        IDLE,
        /**
         * We have sent our Open message, and are waiting for the peer's Open message.
         */
        OPEN_SENT,
        /**
         * We have received the peer's Open message, which is acceptable, and we're waiting the acknowledgement of our
         * Open message.
         */
        OPEN_CONFIRM,
        /**
         * The negotiation finished.
         */
        FINISHED,
    }

    AbstractBGPSessionNegotiator(final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPPeerRegistry registry) {
        this.promise = requireNonNull(promise);
        this.channel = requireNonNull(channel);
        this.registry = registry;
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private synchronized void startNegotiation() {
        LOG.debug("Starting negotiating with {}, current state: {}", channel.remoteAddress(), state);
        if (state != State.IDLE && state != State.OPEN_CONFIRM) {
            return;
        }
        // Open can be sent first either from ODL (IDLE) or from peer (OPEN_CONFIRM)
        final var remoteIp = getRemoteIp();
        try {
            // Check if peer is configured in registry before retrieving preferences
            if (!registry.isPeerConfigured(remoteIp)) {
                negotiationFailed(new BGPDocumentedException(
                    "BGP peer with ip: %s not configured, check configured peers in : %s".formatted(
                        remoteIp, registry), BGPError.CONNECTION_REJECTED));
                return;
            }

            final var preferences = registry.getPeerPreferences(remoteIp);
            final var as = openASNumber(preferences.getMyAs().getValue().longValue());
            sendMessage(new OpenBuilder().setMyAsNumber(as).setHoldTimer(Uint16.valueOf(preferences.getHoldTime()))
                .setBgpIdentifier(preferences.getBgpId()).setBgpParameters(preferences.getParams()).build());
            if (state != State.FINISHED) {
                state = State.OPEN_SENT;
                pending = channel.eventLoop().schedule(() -> {
                    synchronized (AbstractBGPSessionNegotiator.this) {
                        pending = null;
                        if (state != State.FINISHED) {
                            sendMessage(buildErrorNotify(BGPError.HOLD_TIMER_EXPIRED));
                            negotiationFailed(new BGPDocumentedException("HoldTimer expired", BGPError.FSM_ERROR));
                            state = State.FINISHED;
                        }
                    }
                }, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            LOG.warn("Unexpected negotiation failure", e);
            negotiationFailedCloseChannel(e);
        }
    }

    private IpAddressNoZone getRemoteIp() {
        final var remoteIp = StrictBGPPeerRegistry.getIpAddress(channel.remoteAddress());
        if (remoteIp.getIpv6AddressNoZone() != null) {
            return new IpAddressNoZone(Ipv6Util.getFullForm(remoteIp.getIpv6AddressNoZone()));
        }
        return remoteIp;
    }

    synchronized void handleMessage(final Notification<?> msg) {
        LOG.debug("Channel {} handling message in state {}, msg: {}", channel, state, msg);
        switch (state) {
            case null -> throw new NullPointerException();
            case FINISHED -> {
                sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
                return;
            }
            case IDLE -> {
                // to avoid race condition when Open message was sent by the peer before startNegotiation could be
                // executed
                if (msg instanceof Open open) {
                    startNegotiation();
                    handleOpen(open);
                    return;
                }
                sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
            }
            case OPEN_CONFIRM -> {
                switch (msg) {
                    case Keepalive ka -> {
                        negotiationSuccessful();
                        LOG.info("BGP Session with peer {} established successfully.", channel);
                    }
                    case Notify ntf ->
                        negotiationFailed(new BGPDocumentedException("Peer refusal",
                            BGPError.forValue(ntf.getErrorCode(), ntf.getErrorSubcode())));
                    default -> {
                        // no-op
                    }
                }
                state = State.FINISHED;
                return;
            }
            case OPEN_SENT -> {
                if (msg instanceof Open open) {
                    handleOpen(open);
                    return;
                }
            }
        }

        // Catch-all for unexpected message
        LOG.warn("Channel {} state {} unexpected message {}", channel, state, msg);
        sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
        negotiationFailed(new BGPDocumentedException("Unexpected message channel: "
                + channel + ", state: " + state + ", message: " + msg, BGPError.FSM_ERROR));
        state = State.FINISHED;
    }

    private static Notify buildErrorNotify(final BGPError err) {
        return buildErrorNotify(err, null);
    }

    private static Notify buildErrorNotify(final BGPError err, final byte[] data) {
        final var builder = new NotifyBuilder().setErrorCode(err.getCode()).setErrorSubcode(err.getSubcode());
        if (data != null && data.length != 0) {
            builder.setData(data);
        }
        return builder.build();
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private synchronized void handleOpen(final Open openObj) {
        final var remoteIp = getRemoteIp();
        final var preferences = registry.getPeerPreferences(remoteIp);
        try {
            final var peer = registry.getPeer(remoteIp, getSourceId(openObj, preferences),
                    getDestinationId(openObj, preferences), openObj);
            sendMessage(new KeepaliveBuilder().build());
            state = State.OPEN_CONFIRM;
            session = new BGPSessionImpl(peer, channel, openObj, preferences, registry);
            session.setChannelExtMsgCoder(openObj);
            LOG.debug("Channel {} moved to OPEN_CONFIRM state with remote proposal {}", channel, openObj);
        } catch (final BGPDocumentedException | RuntimeException e) {
            LOG.warn("Channel {} negotiation failed", channel, e);
            negotiationFailed(e);
        }
    }

    private synchronized void negotiationFailed(final Throwable cause) {
        LOG.warn("Channel {} negotiation failed: {}", channel, cause.getMessage());
        if (cause instanceof BGPDocumentedException documented) {
            // although sendMessage() can also result in calling this method, it won't create a cycle.
            // In case sendMessage() fails to deliver the message, this method gets called with different
            // exception (definitely not with BGPDocumentedException).
            sendMessage(buildErrorNotify(documented.getError(), documented.getData()));
        }
        if (state == State.OPEN_CONFIRM) {
            registry.removePeerSession(getRemoteIp());
        }
        negotiationFailedCloseChannel(cause);
        state = State.FINISHED;
    }

    /**
     * Get destination identifier.
     *
     * @param openMsg Open message received from remote BGP speaker
     * @param preferences Local BGP speaker preferences
     * @return BGP Id of device that accepted the connection
     */
    protected abstract Ipv4AddressNoZone getDestinationId(Open openMsg, BGPSessionPreferences preferences);

    /**
     * Get source identifier.
     *
     * @param openMsg Open message received from remote BGP speaker
     * @param preferences Local BGP speaker preferences
     * @return BGP Id of device that accepted the connection
     */
    protected abstract Ipv4AddressNoZone getSourceId(Open openMsg, BGPSessionPreferences preferences);

    public synchronized State getState() {
        return state;
    }

    @Holding("this")
    private void negotiationSuccessful() {
        LOG.debug("Negotiation on channel {} successful with session {}", channel, session);
        channel.pipeline().replace(this, "session", session);
        promise.setSuccess(session);
    }

    private void negotiationFailedCloseChannel(final Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", channel, cause);
        channel.close();
        synchronized (AbstractBGPSessionNegotiator.this) {
            if (pending != null && pending.isCancellable()) {
                pending.cancel(true);
                pending = null;
            }
        }
    }

    private void sendMessage(final Notification<?> msg) {
        channel.writeAndFlush(msg).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                LOG.warn("Failed to send message {} to channel {}", msg, channel, f.cause());
                negotiationFailedCloseChannel(f.cause());
            } else {
                LOG.trace("Message {} sent to channel {}", msg, channel);
            }
        });
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        LOG.debug("Starting session negotiation on channel {}", channel);
        startNegotiation();
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LOG.debug("Negotiation read invoked on channel {}", channel);
        try {
            handleMessage((Notification<?>) msg);
        } catch (final Exception e) {
            LOG.debug("Unexpected error while handling negotiation message {}", msg, e);
            negotiationFailedCloseChannel(e);
        }

    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.info("Unexpected error during negotiation", cause);
        negotiationFailedCloseChannel(cause);
    }

    @VisibleForTesting
    static Uint16 openASNumber(final long configuredASNumber) {
        // Return AS_TRANS if the value is bigger than 2B.
        return configuredASNumber > Values.UNSIGNED_SHORT_MAX_VALUE ? AS_TRANS : Uint16.valueOf(configuredASNumber);
    }
}
