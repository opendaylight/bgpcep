/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.SessionNegotiator;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bgp Session negotiator. Common for local-to-remote and remote-to-local connections.
 * One difference is session validation performed by injected BGPSessionValidator when OPEN message is received.
 */
public abstract class AbstractBGPSessionNegotiator extends ChannelInboundHandlerAdapter implements SessionNegotiator {
    // 4 minutes recommended in http://tools.ietf.org/html/rfc4271#section-8.2.2
    private static final int INITIAL_HOLDTIMER = 4;

    /**
     * @see <a href="http://tools.ietf.org/html/rfc6793">BGP Support for 4-Octet AS Number Space</a>
     */
    private static final int AS_TRANS = 23456;

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

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBGPSessionNegotiator.class);
    private final BGPPeerRegistry registry;
    private final Promise<BGPSessionImpl> promise;
    private final Channel channel;
    @GuardedBy("this")
    private State state = State.IDLE;

    @GuardedBy("this")
    private BGPSessionImpl session;

    public AbstractBGPSessionNegotiator(final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPPeerRegistry registry) {
        this.promise = Preconditions.checkNotNull(promise);
        this.channel = Preconditions.checkNotNull(channel);
        this.registry = registry;
    }

    private synchronized void startNegotiation() {
        // Open can be sent first either from ODL (IDLE) or from peer (OPEN_CONFIRM)
        Preconditions.checkState(this.state == State.IDLE || this.state == State.OPEN_CONFIRM);
        final IpAddress remoteIp = getRemoteIp();

        // Check if peer is configured in registry before retrieving preferences
        if (!this.registry.isPeerConfigured(remoteIp)) {
            final BGPDocumentedException cause = new BGPDocumentedException(
                String.format("BGP peer with ip: %s not configured, check configured peers in : %s", remoteIp, this.registry), BGPError.CONNECTION_REJECTED);
            negotiationFailed(cause);
            return;
        }

        final BGPSessionPreferences preferences = this.registry.getPeerPreferences(remoteIp);

        int as = preferences.getMyAs().getValue().intValue();
        // Set as AS_TRANS if the value is bigger than 2B
        if (as > Values.UNSIGNED_SHORT_MAX_VALUE) {
            as = AS_TRANS;
        }
        sendMessage(new OpenBuilder().setMyAsNumber(as).setHoldTimer(preferences.getHoldTime()).setBgpIdentifier(
                preferences.getBgpId()).setBgpParameters(preferences.getParams()).build());
        if (this.state != State.FINISHED) {
            this.state = State.OPEN_SENT;

            this.channel.eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    if (AbstractBGPSessionNegotiator.this.state != State.FINISHED) {
                        AbstractBGPSessionNegotiator.this.sendMessage(buildErrorNotify(BGPError.HOLD_TIMER_EXPIRED));
                        negotiationFailed(new BGPDocumentedException("HoldTimer expired", BGPError.FSM_ERROR));
                        AbstractBGPSessionNegotiator.this.state = State.FINISHED;
                    }
                }
            }, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
        }
    }

    private IpAddress getRemoteIp() {
        return StrictBGPPeerRegistry.getIpAddress(this.channel.remoteAddress());
    }

    protected synchronized void handleMessage(final Notification msg) {
        LOG.debug("Channel {} handling message in state {}", this.channel, this.state);

        switch (this.state) {
        case FINISHED:
            sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
            return;
        case IDLE:
            // to avoid race condition when Open message was sent by the peer before startNegotiation could be executed
            if (msg instanceof Open) {
                handleOpen((Open) msg);
                return;
            }
            sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
            return;
        case OPEN_CONFIRM:
            if (msg instanceof Keepalive) {
                negotiationSuccessful(this.session);
                LOG.info("BGP Session with peer {} established successfully.", this.channel);
            } else if (msg instanceof Notify) {
                final Notify ntf = (Notify) msg;
                negotiationFailed(new BGPDocumentedException("Peer refusal", BGPError.forValue(ntf.getErrorCode(), ntf.getErrorSubcode())));
            }
            this.state = State.FINISHED;
            return;
        case OPEN_SENT:
            if (msg instanceof Open) {
                handleOpen((Open) msg);
                return;
            }
            break;
        default:
            break;
        }

        // Catch-all for unexpected message
        LOG.warn("Channel {} state {} unexpected message {}", this.channel, this.state, msg);
        sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
        negotiationFailed(new BGPDocumentedException("Unexpected message", BGPError.FSM_ERROR));
        this.state = State.FINISHED;
    }

    private static Notify buildErrorNotify(final BGPError err) {
        return buildErrorNotify(err, null);
    }

    private static Notify buildErrorNotify(final BGPError err, final byte[] data) {
        final NotifyBuilder builder = new NotifyBuilder().setErrorCode(err.getCode()).setErrorSubcode(err.getSubcode());
        if (data != null && data.length != 0) {
            builder.setData(data);
        }
        return builder.build();
    }

    private void handleOpen(final Open openObj) {
        final IpAddress remoteIp = getRemoteIp();
        final BGPSessionPreferences preferences = this.registry.getPeerPreferences(remoteIp);
        try {
            final BGPSessionListener peer = this.registry.getPeer(remoteIp, getSourceId(openObj, preferences), getDestinationId(openObj, preferences), openObj);
            sendMessage(new KeepaliveBuilder().build());
            this.session = new BGPSessionImpl(peer, this.channel, openObj, preferences, this.registry);
            this.state = State.OPEN_CONFIRM;
            LOG.debug("Channel {} moved to OpenConfirm state with remote proposal {}", this.channel, openObj);
        } catch (final BGPDocumentedException e) {
            LOG.warn("Channel {} negotiation failed", this.channel, e);
            negotiationFailed(e);
        }
    }

    private void negotiationFailed(final Throwable e) {
        LOG.warn("Channel {} negotiation failed: {}", this.channel, e.getMessage());
        if (e instanceof BGPDocumentedException) {
            // although sendMessage() can also result in calling this method, it won't create a cycle. In case sendMessage() fails to
            // deliver the message, this method gets called with different exception (definitely not with BGPDocumentedException).
            sendMessage(buildErrorNotify(((BGPDocumentedException)e).getError(), ((BGPDocumentedException) e).getData()));
        }
        this.registry.removePeerSession(getRemoteIp());
        negotiationFailedCloseChannel(e);
        this.state = State.FINISHED;
    }

    /**
     * @param openMsg Open message received from remote BGP speaker
     * @param preferences Local BGP speaker preferences
     * @return BGP Id of device that accepted the connection
     */
    protected abstract Ipv4Address getDestinationId(final Open openMsg, final BGPSessionPreferences preferences);

    /**
     * @param openMsg Open message received from remote BGP speaker
     * @param preferences Local BGP speaker preferences
     * @return BGP Id of device that accepted the connection
     */
    protected abstract Ipv4Address getSourceId(final Open openMsg, final BGPSessionPreferences preferences);

    public synchronized State getState() {
        return this.state;
    }

    private void negotiationSuccessful(final BGPSessionImpl session) {
        LOG.debug("Negotiation on channel {} successful with session {}", this.channel, session);
        this.channel.pipeline().replace(this, "session", session);
        this.promise.setSuccess(session);
    }

    private void negotiationFailedCloseChannel(final Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", this.channel, cause);
        this.channel.close();
        this.promise.setFailure(cause);
    }

    private void sendMessage(final Notification msg) {
        this.channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture f) {
                if (!f.isSuccess()) {
                    LOG.warn("Failed to send message {}", msg, f.cause());
                    negotiationFailedCloseChannel(f.cause());
                } else {
                    LOG.trace("Message {} sent to socket", msg);
                }

            }
        });
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        LOG.debug("Starting session negotiation on channel {}", this.channel);

        try {
            startNegotiation();
        } catch (final Exception e) {
            LOG.warn("Unexpected negotiation failure", e);
            negotiationFailedCloseChannel(e);
        }

    }

    @Override
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LOG.debug("Negotiation read invoked on channel {}", this.channel);

        try {
            handleMessage((Notification) msg);
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
}
