/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Abstract PCEP session negotiator. Takes care of basic handshake without implementing a specific policy. Policies need
 * to be provided by a specific subclass.
 */
public abstract class AbstractPCEPSessionNegotiator extends AbstractSessionNegotiator<Message, PCEPSessionImpl> {
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
		Idle,
		/**
		 * Waiting for the peer's OPEN message.
		 */
		OpenWait,
		/**
		 * Waiting for the peer's KEEPALIVE message.
		 */
		KeepWait,
		/**
		 * Negotiation has completed.
		 */
		Finished,
	}

	private static final Logger logger = LoggerFactory.getLogger(AbstractPCEPSessionNegotiator.class);

	private final Timer timer;

	@GuardedBy("this")
	private State state = State.Idle;

	@GuardedBy("this")
	private Timeout failTimer;

	@GuardedBy("this")
	private Open localPrefs;

	@GuardedBy("this")
	private Open remotePrefs;

	private volatile boolean localOK, openRetry, remoteOK;

	private final Keepalive keepalive = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build();

	protected AbstractPCEPSessionNegotiator(final Timer timer, final Promise<PCEPSessionImpl> promise, final Channel channel) {
		super(promise, channel);
		this.timer = Preconditions.checkNotNull(timer);
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
	 * @param timer Timer which the session can use for its various functions.
	 * @param channel Underlying channel.
	 * @param sessionId Assigned session ID.
	 * @param localPrefs Session preferences proposed by us and accepted by the peer.
	 * @param remotePrefs Session preferences proposed by the peer and accepted by us.
	 * @return New protocol session.
	 */
	protected abstract PCEPSessionImpl createSession(Timer timer, Channel channel, Open localPrefs, Open remotePrefs);

	/**
	 * Sends PCEP Error Message with one PCEPError.
	 * 
	 * @param value
	 */
	private void sendErrorMessage(final PCEPErrors value) {

		this.channel.writeAndFlush(Util.createErrorMessage(value, null));
	}

	private void scheduleFailTimer() {
		final Object lock = this;

		this.failTimer = this.timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				synchronized (lock) {
					// This closes the race between timer expiring and new timer
					// being armed while it waits for the lock.
					if (AbstractPCEPSessionNegotiator.this.failTimer == timeout) {
						switch (AbstractPCEPSessionNegotiator.this.state) {
						case Finished:
						case Idle:
							break;
						case KeepWait:
							sendErrorMessage(PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT);
							negotiationFailed(new TimeoutException("KeepWait timer expired"));
							AbstractPCEPSessionNegotiator.this.state = State.Finished;
							break;
						case OpenWait:
							sendErrorMessage(PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT);
							negotiationFailed(new TimeoutException("OpenWait timer expired"));
							AbstractPCEPSessionNegotiator.this.state = State.Finished;
							break;
						}
					}
				}
			}
		}, FAIL_TIMER_VALUE, TimeUnit.SECONDS);
	}

	@Override
	final synchronized protected void startNegotiation() {
		Preconditions.checkState(this.state == State.Idle);
		this.localPrefs = getInitialProposal();
		final OpenMessage m = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder().setOpenMessage(
				new OpenMessageBuilder().setOpen(this.localPrefs).build()).build();
		this.channel.writeAndFlush(m);
		this.state = State.OpenWait;
		scheduleFailTimer();

		logger.debug("Channel {} started sent proposal {}", this.channel, this.localPrefs);
	}

	@Override
	final synchronized protected void handleMessage(final Message msg) throws Exception {
		this.failTimer.cancel();

		logger.debug("Channel {} handling message in state {}", this.channel, this.state);

		switch (this.state) {
		case Finished:
		case Idle:
			throw new IllegalStateException("Unexpected handleMessage in state " + this.state);
		case KeepWait:
			if (msg instanceof Keepalive) {
				this.localOK = true;
				if (this.remoteOK) {
					negotiationSuccessful(createSession(this.timer, this.channel, this.localPrefs, this.remotePrefs));
					this.state = State.Finished;
				} else {
					scheduleFailTimer();
					this.state = State.OpenWait;
					logger.debug("Channel {} moved to OpenWait state with localOK=1", this.channel);
				}

				return;
			} else if (msg instanceof Pcerr) {
				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessage err = ((Pcerr) msg).getPcerrMessage();
				this.localPrefs = getRevisedProposal(((Session) err.getErrorType()).getOpen());
				if (this.localPrefs == null) {
					sendErrorMessage(PCEPErrors.PCERR_NON_ACC_SESSION_CHAR);
					negotiationFailed(new RuntimeException("Peer suggested unacceptable retry proposal"));
					this.state = State.Finished;
					return;
				}
				this.channel.writeAndFlush(new OpenBuilder().setOpenMessage(new OpenMessageBuilder().setOpen(this.localPrefs).build()).build());
				if (!this.remoteOK) {
					this.state = State.OpenWait;
				}
				scheduleFailTimer();
				return;
			}

			break;
		case OpenWait:
			if (msg instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open) {
				final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessage o = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open) msg).getOpenMessage();
				final Open open = o.getOpen();
				if (isProposalAcceptable(open)) {
					this.channel.writeAndFlush(this.keepalive);
					this.remotePrefs = open;
					this.remoteOK = true;
					if (this.localOK) {
						negotiationSuccessful(createSession(this.timer, this.channel, this.localPrefs, this.remotePrefs));
						this.state = State.Finished;
					} else {
						scheduleFailTimer();
						this.state = State.KeepWait;
						logger.debug("Channel {} moved to KeepWait state with remoteOK=1", this.channel);
					}
					return;
				}

				if (this.openRetry) {
					sendErrorMessage(PCEPErrors.SECOND_OPEN_MSG);
					negotiationFailed(new RuntimeException("OPEN renegotiation failed"));
					this.state = State.Finished;
					return;
				}

				final Open newPrefs = getCounterProposal(open);
				if (newPrefs == null) {
					sendErrorMessage(PCEPErrors.NON_ACC_NON_NEG_SESSION_CHAR);
					negotiationFailed(new RuntimeException("Peer sent unacceptable session parameters"));
					this.state = State.Finished;
					return;
				}

				this.channel.writeAndFlush(Util.createErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR, newPrefs));

				this.openRetry = true;
				this.state = this.localOK ? State.OpenWait : State.KeepWait;
				scheduleFailTimer();
				return;
			}

			break;
		}

		logger.warn("Channel {} in state {} received unexpected message {}", this.channel, this.state, msg);
		sendErrorMessage(PCEPErrors.NON_OR_INVALID_OPEN_MSG);
		negotiationFailed(new Exception("Illegal message encountered"));
		this.state = State.Finished;
	}

	public synchronized State getState() {
		return this.state;
	}
}
