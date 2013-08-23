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
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPKeepAliveMessage;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Abstract PCEP session negotiator. Takes care of basic handshake without
 * implementing a specific policy. Policies need to be provided by a specific
 * subclass.
 */
public abstract class AbstractPCEPSessionNegotiator extends AbstractSessionNegotiator<PCEPMessage, PCEPSessionImpl> {
	/**
	 * Unified KeepWait and OpenWait timer expiration, in seconds.
	 */
	public static final int FAIL_TIMER_VALUE = 60;

	/**
	 * PCEP session negotiation state transitions are described in RFC5440.
	 * Simplification the two timers (KeepWait and OpenWait) are merged into
	 * a FailTimer, as they are mutually exclusive, have the same timeout
	 * value and their action is to terminate negotiation. This timer is
	 * restarted between state transitions and runs in all states except
	 * Idle and Finished.
	 */
	private enum State {
		/**
		 * Negotiation has not begun. It will be activated once we are asked
		 * to provide our initial proposal, at which point we move into
		 * OpenWait state.
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
	private PCEPOpenObject localPrefs;

	@GuardedBy("this")
	private PCEPOpenObject remotePrefs;

	private volatile boolean localOK, openRetry, remoteOK;

	protected AbstractPCEPSessionNegotiator(final Timer timer, final Promise<PCEPSessionImpl> promise, final Channel channel) {
		super(promise, channel);
		this.timer = Preconditions.checkNotNull(timer);
	}

	/**
	 * Get the initial session parameters proposal.
	 * @return Session parameters proposal.
	 */
	protected abstract PCEPOpenObject getInitialProposal();

	/**
	 * Get the revised session parameters proposal based on the feedback
	 * the peer has provided to us.
	 * 
	 * @param suggestion Peer-provided suggested session parameters
	 * @return Session parameters proposal.
	 */
	protected abstract PCEPOpenObject getRevisedProposal(PCEPOpenObject suggestion);

	/**
	 * Check whether a peer-provided session parameters proposal is acceptable.
	 * 
	 * @param proposal peer-proposed session parameters
	 * @return true if the proposal is acceptable, false otherwise
	 */
	protected abstract boolean isProposalAcceptable(PCEPOpenObject proposal);

	/**
	 * Given a peer-provided session parameters proposal which we found
	 * unacceptable, provide a counter-proposal. The requirement is that
	 * the isProposalAcceptable() method has to return true when presented
	 * with this proposal.
	 * 
	 * @param proposal unacceptable peer proposal
	 * @return our counter-proposal, or null if there is no way to negotiate
	 *         an acceptable proposal
	 */
	protected abstract PCEPOpenObject getCounterProposal(PCEPOpenObject proposal);

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
	protected abstract PCEPSessionImpl createSession(Timer timer, Channel channel,
			PCEPOpenObject localPrefs, PCEPOpenObject remotePrefs);

	/**
	 * Sends PCEP Error Message with one PCEPError.
	 * 
	 * @param value
	 */
	private void sendErrorMessage(final PCEPErrors value) {
		channel.writeAndFlush(new PCEPErrorMessage(ImmutableList.of(new PCEPErrorObject(value))));
	}

	private void scheduleFailTimer() {
		final Object lock = this;

		failTimer = timer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				synchronized (lock) {
					// This closes the race between timer expiring and new timer
					// being armed while it waits for the lock.
					if (failTimer == timeout) {
						switch (state) {
						case Finished:
						case Idle:
							break;
						case KeepWait:
							sendErrorMessage(PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT);
							negotiationFailed(new TimeoutException("KeepWait timer expired"));
							state = State.Finished;
							break;
						case OpenWait:
							sendErrorMessage(PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT);
							negotiationFailed(new TimeoutException("OpenWait timer expired"));
							state = State.Finished;
							break;
						}
					}
				}
			}
		}, FAIL_TIMER_VALUE, TimeUnit.SECONDS);
	}

	@Override
	final synchronized protected void startNegotiation() {
		Preconditions.checkState(state == State.Idle);
		localPrefs = getInitialProposal();
		channel.writeAndFlush(new PCEPOpenMessage(localPrefs));
		state = State.OpenWait;
		scheduleFailTimer();

		logger.debug("Channel {} started sent proposal {}", channel, localPrefs);
	}

	@Override
	final synchronized protected void handleMessage(final PCEPMessage msg) throws Exception {
		failTimer.cancel();

		logger.debug("Channel {} handling message in state {}", channel, msg);

		switch (state) {
		case Finished:
		case Idle:
			throw new IllegalStateException("Unexpected handleMessage in state " + state);
		case KeepWait:
			if (msg instanceof PCEPKeepAliveMessage) {
				localOK = true;
				if (remoteOK) {
					negotiationSuccessful(createSession(timer, channel, localPrefs, remotePrefs));
					state = State.Finished;
				} else {
					scheduleFailTimer();
					state = State.OpenWait;
					logger.debug("Channel {} moved to OpenWait state with localOK=1", channel);
				}

				return;
			} else if (msg instanceof PCEPErrorMessage) {
				final PCEPErrorMessage err = (PCEPErrorMessage) msg;
				localPrefs = getRevisedProposal(err.getOpenObject());
				if (localPrefs == null) {
					sendErrorMessage(PCEPErrors.PCERR_NON_ACC_SESSION_CHAR);
					negotiationFailed(new RuntimeException("Peer suggested unacceptable retry proposal"));
					state = State.Finished;
					return;
				}

				if (!remoteOK) {
					state = State.OpenWait;
				}
				scheduleFailTimer();
				return;
			}

			break;
		case OpenWait:
			if (msg instanceof PCEPOpenMessage) {
				final PCEPOpenObject open = ((PCEPOpenMessage) msg).getOpenObject();
				if (isProposalAcceptable(open)) {
					channel.writeAndFlush(new PCEPKeepAliveMessage());
					remotePrefs = open;
					remoteOK = true;
					if (localOK) {
						negotiationSuccessful(createSession(timer, channel, localPrefs, remotePrefs));
						state = State.Finished;
					} else {
						scheduleFailTimer();
						state = State.KeepWait;
						logger.debug("Channel {} moved to KeepWait state with remoteOK=1", channel);
					}
					return;
				}

				if (openRetry) {
					sendErrorMessage(PCEPErrors.SECOND_OPEN_MSG);
					negotiationFailed(new RuntimeException("OPEN renegotiation failed"));
					state = State.Finished;
					return;
				}

				final PCEPOpenObject newPrefs = getCounterProposal(open);
				if (newPrefs == null) {
					sendErrorMessage(PCEPErrors.NON_ACC_NON_NEG_SESSION_CHAR);
					negotiationFailed(new RuntimeException("Peer sent unacceptable session parameters"));
					state = State.Finished;
					return;
				}

				channel.writeAndFlush(
						new PCEPErrorMessage(newPrefs, ImmutableList.of(
								new PCEPErrorObject(PCEPErrors.NON_ACC_NEG_SESSION_CHAR)), null));

				openRetry = true;
				state = localOK ? State.OpenWait : State.KeepWait;
				scheduleFailTimer();
				return;
			}

			break;
		}

		logger.warn("Channel {} in state {} received unexpected message {}", channel, state, msg);
		sendErrorMessage(PCEPErrors.NON_OR_INVALID_OPEN_MSG);
		negotiationFailed(new Exception("Illegal message encountered"));
		state = State.Finished;
	}
}
