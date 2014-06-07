/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Promise;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bgp.parser.AsNumberUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPSessionNegotiator extends AbstractSessionNegotiator<Notification, BGPSessionImpl> {
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
        Idle,
        /**
         * We have sent our Open message, and are waiting for the peer's Open message.
         */
        OpenSent,
        /**
         * We have received the peer's Open message, which is acceptable, and we're waiting the acknowledgement of our
         * Open message.
         */
        OpenConfirm,
        /**
         * The negotiation finished.
         */
        Finished,
    }

    private static final Logger LOG = LoggerFactory.getLogger(BGPSessionNegotiator.class);
    private final BGPSessionPreferences localPref;
    private final BGPSessionListener listener;
    private final AsNumber remoteAs;
    private final Timer timer;

    @GuardedBy("this")
    private State state = State.Idle;

    @GuardedBy("this")
    private BGPSessionImpl session;

    public BGPSessionNegotiator(final Timer timer, final Promise<BGPSessionImpl> promise, final Channel channel,
            final BGPSessionPreferences initialPrefs, final AsNumber remoteAs, final BGPSessionListener listener) {
        super(promise, channel);
        this.listener = Preconditions.checkNotNull(listener);
        this.localPref = Preconditions.checkNotNull(initialPrefs);
        this.remoteAs = Preconditions.checkNotNull(remoteAs);
        this.timer = Preconditions.checkNotNull(timer);
    }

    @Override
    protected void startNegotiation() {
        Preconditions.checkState(this.state == State.Idle);
        int as = this.localPref.getMyAs().getValue().intValue();
        // Set as AS_TRANS if the value is bigger than 2B
        if (as > Values.UNSIGNED_SHORT_MAX_VALUE) {
            as = AS_TRANS;
        }
        this.sendMessage(new OpenBuilder().setMyAsNumber(as).setHoldTimer(this.localPref.getHoldTime()).setBgpIdentifier(
                this.localPref.getBgpId()).setBgpParameters(this.localPref.getParams()).build());
        this.state = State.OpenSent;

        final Object lock = this;
        this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) {
                synchronized (lock) {
                    if (BGPSessionNegotiator.this.state != State.Finished) {
                        BGPSessionNegotiator.this.sendMessage(buildErrorNotify(BGPError.HOLD_TIMER_EXPIRED));
                        negotiationFailed(new BGPDocumentedException("HoldTimer expired", BGPError.FSM_ERROR));
                        BGPSessionNegotiator.this.state = State.Finished;
                    }
                }
            }
        }, INITIAL_HOLDTIMER, TimeUnit.MINUTES);
    }

    @Override
    protected synchronized void handleMessage(final Notification msg) {
        LOG.debug("Channel {} handling message in state {}", this.channel, this.state);

        switch (this.state) {
        case Finished:
        case Idle:
            this.sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
            return;
        case OpenConfirm:
            if (msg instanceof Keepalive) {
                negotiationSuccessful(this.session);
                LOG.info("BGP Session with peer {} established successfully.", this.channel);
            } else if (msg instanceof Notify) {
                final Notify ntf = (Notify) msg;
                negotiationFailed(new BGPDocumentedException("Peer refusal", BGPError.forValue(ntf.getErrorCode(), ntf.getErrorSubcode())));
            }
            this.state = State.Finished;
            return;
        case OpenSent:
            if (msg instanceof Open) {
                final Open openObj = (Open) msg;
                handleOpen(openObj);
                return;
            }
            break;
        }

        // Catch-all for unexpected message
        LOG.warn("Channel {} state {} unexpected message {}", this.channel, this.state, msg);
        this.sendMessage(buildErrorNotify(BGPError.FSM_ERROR));
        negotiationFailed(new BGPDocumentedException("Unexpected message", BGPError.FSM_ERROR));
        this.state = State.Finished;
    }

    private static Notify buildErrorNotify(final BGPError err) {
        return new NotifyBuilder().setErrorCode(err.getCode()).setErrorSubcode(err.getSubcode()).build();
    }

    private void handleOpen(final Open openObj) {
        final AsNumber as = AsNumberUtil.advertizedAsNumber(openObj);
        if (!this.remoteAs.equals(as)) {
            LOG.warn("Unexpected remote AS number. Expecting {}, got {}", this.remoteAs, as);
            this.sendMessage(buildErrorNotify(BGPError.BAD_PEER_AS));
            negotiationFailed(new BGPDocumentedException("Peer AS number mismatch", BGPError.BAD_PEER_AS));
            this.state = State.Finished;
            return;
        }

        final List<BgpParameters> prefs = openObj.getBgpParameters();
        if (prefs != null && !prefs.isEmpty()) {
            if (!prefs.containsAll(this.localPref.getParams())) {
                LOG.info("BGP Open message session parameters differ, session still accepted.");
            }
            this.sendMessage(new KeepaliveBuilder().build());
            this.session = new BGPSessionImpl(this.timer, this.listener, this.channel, openObj, this.localPref.getHoldTime());
            this.state = State.OpenConfirm;
            LOG.debug("Channel {} moved to OpenConfirm state with remote proposal {}", this.channel, openObj);
            return;
        }

        this.sendMessage(buildErrorNotify(BGPError.UNSPECIFIC_OPEN_ERROR));
        negotiationFailed(new BGPDocumentedException("Open message unacceptable. Check the configuration of BGP speaker.", BGPError.UNSPECIFIC_OPEN_ERROR));
        this.state = State.Finished;
    }

    public synchronized State getState() {
        return this.state;
    }
}
