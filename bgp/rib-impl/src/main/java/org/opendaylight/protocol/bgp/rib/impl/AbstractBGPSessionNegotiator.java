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
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
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
 * Bgp Session negotiator. Common for local -> remote and remote -> local connections.
 * One difference is session validation performed by injected BGPSessionValidator when OPEN message is received.
 */
public abstract class AbstractBGPSessionNegotiator extends AbstractSessionNegotiator<Notification, BGPSessionImpl> {
    // 4 minutes recommended in http://tools.ietf.org/html/rfc4271#section-8.2.2
    protected static final int INITIAL_HOLDTIMER = 4;

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
    private final BGPSessionValidator sessionValidator;

    @GuardedBy("this")
    private State state = State.IDLE;

    @GuardedBy("this")
    private BGPSessionImpl session;

    public AbstractBGPSessionNegotiator(final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPPeerRegistry registry, final BGPSessionValidator sessionValidator) {
        super(promise, channel);
        this.registry = registry;
        this.sessionValidator = sessionValidator;
    }

    @Override
    protected synchronized void startNegotiation() {
        Preconditions.checkState(this.state == State.IDLE);

        // Check if peer is configured in registry before retrieving preferences
        if (!this.registry.isPeerConfigured(getRemoteIp())) {
            final BGPDocumentedException cause = new BGPDocumentedException(
                    "BGP peer with ip: " + getRemoteIp()
                    + " not configured, check configured peers in : "
                    + this.registry, BGPError.CONNECTION_REJECTED);
            negotiationFailed(cause);
            return;
        }

        final BGPSessionPreferences preferences = getPreferences();

        int as = preferences.getMyAs().getValue().intValue();
        // Set as AS_TRANS if the value is bigger than 2B
        if (as > Values.UNSIGNED_SHORT_MAX_VALUE) {
            as = AS_TRANS;
        }
        this.sendMessage(new OpenBuilder().setMyAsNumber(as).setHoldTimer(preferences.getHoldTime()).setBgpIdentifier(
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

    private BGPSessionPreferences getPreferences() {
        return this.registry.getPeerPreferences(getRemoteIp());
    }

    private IpAddress getRemoteIp() {
        return StrictBGPPeerRegistry.getIpAddress(this.channel.remoteAddress());
    }

    @Override
    protected synchronized void handleMessage(final Notification msg) {
        LOG.debug("Channel {} handling message in state {}", this.channel, this.state);

        switch (this.state) {
        case FINISHED:
        case IDLE:
            this.sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
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
                final Open openObj = (Open) msg;
                handleOpen(openObj);
                return;
            }
            break;
        default:
            break;
        }

        // Catch-all for unexpected message
        LOG.warn("Channel {} state {} unexpected message {}", this.channel, this.state, msg);
        this.sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
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
        try {
            this.sessionValidator.validate(openObj, getPreferences());
        } catch (final BGPDocumentedException e) {
            negotiationFailed(e);
            return;
        }

        try {
            final BGPSessionListener peer = this.registry.getPeer(getRemoteIp(), getSourceId(openObj, getPreferences()), getDestinationId(openObj, getPreferences()));
            this.sendMessage(new KeepaliveBuilder().build());
            this.session = new BGPSessionImpl(peer, this.channel, openObj, getPreferences());
            this.state = State.OPEN_CONFIRM;
            LOG.debug("Channel {} moved to OpenConfirm state with remote proposal {}", this.channel, openObj);
        } catch (final BGPDocumentedException e) {
            LOG.warn("Channel {} negotiation failed", this.channel, e);
            negotiationFailed(e);
        }
    }

    @Override
    protected void negotiationFailed(final Throwable e) {
        LOG.warn("Channel {} negotiation failed: {}", this.channel, e.getMessage());
        if (e instanceof BGPDocumentedException) {
            // although sendMessage() can also result in calling this method, it won't create a cycle. In case sendMessage() fails to
            // deliver the message, this method gets called with different exception (definitely not with BGPDocumentedException).
            this.sendMessage(buildErrorNotify(((BGPDocumentedException)e).getError(), ((BGPDocumentedException) e).getData()));
        }
        this.registry.removePeerSession(getRemoteIp());
        super.negotiationFailed(e);
        this.state = State.FINISHED;
    }

    /**
     * @return BGP Id of device that accepted the connection
     */
    protected abstract Ipv4Address getDestinationId(final Open openMsg, final BGPSessionPreferences preferences);

    /**
     * @return BGP Id of device that initiated the connection
     */
    protected abstract Ipv4Address getSourceId(final Open openMsg, final BGPSessionPreferences preferences);

    public synchronized State getState() {
        return this.state;
    }
}
