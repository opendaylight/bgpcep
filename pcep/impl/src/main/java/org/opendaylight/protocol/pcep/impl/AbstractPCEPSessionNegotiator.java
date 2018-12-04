/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.impl.tls.SslContextFactory;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.Tls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Starttls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.StarttlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.start.tls.message.StartTlsMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract PCEP session negotiator. Takes care of basic handshake without implementing a specific policy. Policies need
 * to be provided by a specific subclass.
 */
public abstract class AbstractPCEPSessionNegotiator extends AbstractSessionNegotiator {
    /**
     * Unified KeepWait and OpenWait timer expiration, in seconds.
     */
    public static final int FAIL_TIMER_VALUE = 60;

    /**
     * PCEP session negotiation state transitions are described in RFC5440. Simplification the two timers (KeepWait and
     * OpenWait) are merged into a FailTimer, as they are mutually exclusive, have the same timeout value and their
     * action is to terminate negotiation. This timer is restarted between state transitions and runs in all states
     * except Idle and Finished.
     */
    @VisibleForTesting
    public enum State {
        /**
         * Negotiation has not begun. It will be activated once we are asked to provide our initial proposal, at which
         * point we move into OpenWait state.
         */
        IDLE,
        /**
         * Waiting for the peer's StartTLS message
         */
        START_TLS_WAIT,
        /**
         * Waiting for the peer's OPEN message.
         */
        OPEN_WAIT,
        /**
         * Waiting for the peer's KEEPALIVE message.
         */
        KEEP_WAIT,
        /**
         * Negotiation has completed.
         */
        FINISHED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPSessionNegotiator.class);
    private static final Keepalive KEEPALIVE = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();

    private volatile boolean localOK, openRetry, remoteOK;
    private volatile State state = State.IDLE;
    private Future<?> failTimer;
    private Open localPrefs;
    private Open remotePrefs;
    private Tls tlsConfiguration;

    protected AbstractPCEPSessionNegotiator(final Promise<PCEPSessionImpl> promise, final Channel channel) {
        super(promise, channel);
    }

    /**
     * Get the initial session parameters proposal.
     *
     * @return Session parameters proposal.
     */
    protected abstract Open getInitialProposal();

    /**
     * Get the revised session parameters proposal based on the feedback the peer has provided to us.
     *
     * @param suggestion Peer-provided suggested session parameters
     * @return Session parameters proposal, or null if peers session parameters preclude us from suggesting anything
     */
    protected abstract Open getRevisedProposal(Open suggestion);

    /**
     * Check whether a peer-provided session parameters proposal is acceptable.
     *
     * @param proposal peer-proposed session parameters
     * @return true if the proposal is acceptable, false otherwise
     */
    protected abstract boolean isProposalAcceptable(Open proposal);

    /**
     * Given a peer-provided session parameters proposal which we found unacceptable, provide a counter-proposal. The
     * requirement is that the isProposalAcceptable() method has to return true when presented with this proposal.
     *
     * @param proposal unacceptable peer proposal
     * @return our counter-proposal, or null if there is no way to negotiate an acceptable proposal
     */
    protected abstract Open getCounterProposal(Open proposal);

    /**
     * Create the protocol session.
     *
     * @param channel Underlying channel.
     * @param localPrefs Session preferences proposed by us and accepted by the peer.
     * @param remotePrefs Session preferences proposed by the peer and accepted by us.
     * @return New protocol session.
     */
    protected abstract PCEPSessionImpl createSession(Channel channel, Open localPrefs, Open remotePrefs);

    /**
     * Sends PCEP Error Message with one PCEPError.
     *
     * @param value
     */
    private void sendErrorMessage(final PCEPErrors value) {

        this.sendMessage(Util.createErrorMessage(value, null));
    }

    private void scheduleFailTimer() {
        this.failTimer = this.channel.eventLoop().schedule(() -> {
            switch (AbstractPCEPSessionNegotiator.this.state) {
            case FINISHED:
            case IDLE:
                break;
            case START_TLS_WAIT:
                sendErrorMessage(PCEPErrors.STARTTLS_TIMER_EXP);
                negotiationFailed(new TimeoutException("StartTLSWait timer expired"));
                AbstractPCEPSessionNegotiator.this.state = State.FINISHED;
                break;
            case KEEP_WAIT:
                sendErrorMessage(PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT);
                negotiationFailed(new TimeoutException("KeepWait timer expired"));
                AbstractPCEPSessionNegotiator.this.state = State.FINISHED;
                break;
            case OPEN_WAIT:
                sendErrorMessage(PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT);
                negotiationFailed(new TimeoutException("OpenWait timer expired"));
                AbstractPCEPSessionNegotiator.this.state = State.FINISHED;
                break;
            default:
                break;
            }
        }, FAIL_TIMER_VALUE, TimeUnit.SECONDS);
    }

    @Override
    protected final void startNegotiation() {
        Preconditions.checkState(this.state == State.IDLE);
        if (this.tlsConfiguration != null) {
            this.sendMessage(new StarttlsBuilder().setStartTlsMessage(new StartTlsMessageBuilder().build()).build());
            this.state = State.START_TLS_WAIT;
            scheduleFailTimer();
            LOG.info("Started TLS connection negotiation with peer {}", this.channel);
        } else {
            startNegotiationWithOpen();
        }
        this.channel.closeFuture().addListener((ChannelFutureListener) f -> cancelTimers());
    }

    private void cancelTimers() {
        this.failTimer.cancel(false);
    }

    private void startNegotiationWithOpen() {
        this.localPrefs = getInitialProposal();
        final OpenMessage m = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder().setOpenMessage(
                new OpenMessageBuilder().setOpen(this.localPrefs).build()).build();
        this.sendMessage(m);
        this.state = State.OPEN_WAIT;
        scheduleFailTimer();

        LOG.info("PCEP session with {} started, sent proposal {}", this.channel, this.localPrefs);
    }

    private boolean handleMessageKeepWait(final Message msg) {
        if (msg instanceof Keepalive) {
            return handleMessageKeepAlive();
        } else if (msg instanceof Pcerr) {
            return handleMessagePcerr(msg);
        }
        return false;
    }

    private boolean handleMessageKeepAlive() {
        this.localOK = true;
        if (this.remoteOK) {
            LOG.info("PCEP peer {} completed negotiation", this.channel);
            negotiationSuccessful(createSession(this.channel, this.localPrefs, this.remotePrefs));
            this.state = State.FINISHED;
        } else {
            scheduleFailTimer();
            this.state = State.OPEN_WAIT;
            LOG.debug("Channel {} moved to OpenWait state with localOK=1", this.channel);
        }
        return true;
    }

    private boolean handleMessagePcerr(final Message msg) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessage err = ((Pcerr) msg).getPcerrMessage();
        if (err.getErrorType() == null) {
            final ErrorObject obj = err.getErrors().get(0).getErrorObject();
            LOG.warn("Unexpected error received from PCC: type {} value {}", obj.getType(), obj.getValue());
            negotiationFailed(new IllegalStateException("Unexpected error received from PCC."));
            this.state = State.IDLE;
            return true;
        }
        this.localPrefs = getRevisedProposal(((SessionCase) err.getErrorType()).getSession().getOpen());
        if (this.localPrefs == null) {
            sendErrorMessage(PCEPErrors.PCERR_NON_ACC_SESSION_CHAR);
            negotiationFailed(new IllegalStateException("Peer suggested unacceptable retry proposal"));
            this.state = State.FINISHED;
            return true;
        }
        this.sendMessage(new OpenBuilder().setOpenMessage(new OpenMessageBuilder().setOpen(this.localPrefs).build()).build());
        if (!this.remoteOK) {
            this.state = State.OPEN_WAIT;
        }
        scheduleFailTimer();
        return true;
    }

    private boolean handleMessageOpenWait(final Message msg) {
        if (!(msg instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open)) {
            return false;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.message.OpenMessage o = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open) msg).getOpenMessage();
        final Open open = o.getOpen();
        if (isProposalAcceptable(open)) {
            this.sendMessage(KEEPALIVE);
            this.remotePrefs = open;
            this.remoteOK = true;
            if (this.localOK) {
                negotiationSuccessful(createSession(this.channel, this.localPrefs, this.remotePrefs));
                LOG.info("PCEP peer {} completed negotiation", this.channel);
                this.state = State.FINISHED;
            } else {
                scheduleFailTimer();
                this.state = State.KEEP_WAIT;
                LOG.debug("Channel {} moved to KeepWait state with remoteOK=1", this.channel);
            }
            return true;
        }
        if (this.openRetry) {
            sendErrorMessage(PCEPErrors.SECOND_OPEN_MSG);
            negotiationFailed(new IllegalStateException("OPEN renegotiation failed"));
            this.state = State.FINISHED;
            return true;
        }
        final Open newPrefs = getCounterProposal(open);
        if (newPrefs == null) {
            sendErrorMessage(PCEPErrors.NON_ACC_NON_NEG_SESSION_CHAR);
            negotiationFailed(new IllegalStateException("Peer sent unacceptable session parameters"));
            this.state = State.FINISHED;
            return true;
        }
        this.sendMessage(Util.createErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR, newPrefs));
        this.openRetry = true;
        this.state = this.localOK ? State.OPEN_WAIT : State.KEEP_WAIT;
        scheduleFailTimer();
        return true;
    }

    private boolean handleMessageStartTlsWait(final Message msg) {
        if (msg instanceof Starttls) {
            final SslContextFactory sslFactory = new SslContextFactory(this.tlsConfiguration);
            final SSLContext sslContext = sslFactory.getServerContext();
            if (sslContext == null) {
                this.sendErrorMessage(PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS);
                negotiationFailed(new IllegalStateException("Failed to establish a TLS connection."));
                this.state = State.FINISHED;
                return true;
            }
            final SSLEngine engine = sslContext.createSSLEngine();
            engine.setNeedClientAuth(true);
            engine.setUseClientMode(false);
            this.channel.pipeline().addFirst(new SslHandler(engine));
            LOG.info("PCEPS TLS connection with peer: {} established succesfully.", this.channel);
            startNegotiationWithOpen();
            return true;
        } else if (!(msg instanceof Pcerr)) {
            this.sendErrorMessage(PCEPErrors.NON_STARTTLS_MSG_RCVD);
            negotiationFailed(new IllegalStateException("Unexpected message recieved."));
            this.state = State.FINISHED;
            return true;
        }
        return false;
    }

    @Override
    protected final void handleMessage(final Message msg) {
        cancelTimers();

        LOG.debug("Channel {} handling message {} in state {}", this.channel, msg, this.state);

        switch (this.state) {
        case FINISHED:
        case IDLE:
            throw new IllegalStateException("Unexpected handleMessage in state " + this.state);
        case START_TLS_WAIT:
            if (handleMessageStartTlsWait(msg)) {
                return;
            }
            break;
        case KEEP_WAIT:
            if (handleMessageKeepWait(msg)) {
                return;
            }
            break;
        case OPEN_WAIT:
            if (handleMessageOpenWait(msg)) {
                return;
            }
            break;
        default:
            break;
        }
        LOG.warn("Channel {} in state {} received unexpected message {}", this.channel, this.state, msg);
        sendErrorMessage(PCEPErrors.NON_OR_INVALID_OPEN_MSG);
        negotiationFailed(new Exception("Illegal message encountered"));
        this.state = State.FINISHED;
    }

    @VisibleForTesting
    State getState() {
        return this.state;
    }

    public void setTlsConfiguration(final Tls tlsConfiguration) {
        this.tlsConfiguration = tlsConfiguration;
    }

    @Override
    protected void negotiationFailed(final Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", this.channel, cause);
        this.channel.close();
        this.promise.setFailure(cause);
    }
}
