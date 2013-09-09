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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.message.PCEPCloseMessage;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPKeepAliveMessage;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Implementation of PCEPSession. (Not final for testing.)
 */
class PCEPSessionImpl extends AbstractProtocolSession<PCEPMessage> implements PCEPSession, PCEPSessionRuntimeMXBean {
	/**
	 * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
	 */
	protected volatile long lastMessageSentAt;

	/**
	 * System.nanoTime value about when was received the last message
	 */
	private long lastMessageReceivedAt;

	/**
	 * Protected for testing.
	 */
	protected int maxUnknownMessages;

	protected final Queue<Long> unknownMessagesTimes = new LinkedList<Long>();

	private final PCEPSessionListener listener;

	/**
	 * Open Object with session characteristics that were accepted by another PCE (sent from this session).
	 */
	private final PCEPOpenObject localOpen;

	/**
	 * Open Object with session characteristics for this session (sent from another PCE).
	 */
	private final PCEPOpenObject remoteOpen;

	private static final Logger logger = LoggerFactory.getLogger(PCEPSessionImpl.class);

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	private int sentMsgCount = 0;

	private int receivedMsgCount = 0;

	// True if the listener should not be notified about events
	private boolean closed = false;

	private final Channel channel;

	PCEPSessionImpl(final Timer timer, final PCEPSessionListener listener, final int maxUnknownMessages,
			final Channel channel, final PCEPOpenObject localOpen, final PCEPOpenObject remoteOpen) {
		this.listener = Preconditions.checkNotNull(listener);
		this.stateTimer = Preconditions.checkNotNull(timer);
		this.channel = Preconditions.checkNotNull(channel);
		this.localOpen = Preconditions.checkNotNull(localOpen);
		this.remoteOpen = Preconditions.checkNotNull(remoteOpen);
		this.lastMessageReceivedAt = System.nanoTime();

		if (this.maxUnknownMessages != 0) {
			this.maxUnknownMessages = maxUnknownMessages;
		}

		if (getDeadTimerValue() != 0) {
			stateTimer.newTimeout(new TimerTask() {
				@Override
				public void run(final Timeout timeout) throws Exception {
					handleDeadTimer();
				}
			}, getDeadTimerValue(), TimeUnit.SECONDS);
		}

		if (getKeepAliveTimerValue() != 0) {
			stateTimer.newTimeout(new TimerTask() {
				@Override
				public void run(final Timeout timeout) throws Exception {
					handleKeepaliveTimer();
				}
			}, getKeepAliveTimerValue(), TimeUnit.SECONDS);
		}

		logger.debug("Session started.");
	}

	/**
	 * If DeadTimer expires, the session ends. If a message (whichever) was received during this period, the DeadTimer
	 * will be rescheduled by DEAD_TIMER_VALUE + the time that has passed from the start of the DeadTimer to the time at
	 * which the message was received. If the session was closed by the time this method starts to execute (the session
	 * state will become IDLE), that rescheduling won't occur.
	 */
	private synchronized void handleDeadTimer() {
		final long ct = System.nanoTime();

		final long nextDead = this.lastMessageReceivedAt + TimeUnit.SECONDS.toNanos(getDeadTimerValue());

		if (this.channel.isActive()) {
			if (ct >= nextDead) {
				logger.debug("DeadTimer expired. " + new Date());
				this.terminate(Reason.EXP_DEADTIMER);
			} else {
				stateTimer.newTimeout(new TimerTask() {
					@Override
					public void run(final Timeout timeout) throws Exception {
						handleDeadTimer();
					}
				}, nextDead - ct, TimeUnit.NANOSECONDS);
			}
		}
	}

	/**
	 * If KeepAlive Timer expires, sends KeepAlive message. If a message (whichever) was send during this period, the
	 * KeepAlive Timer will be rescheduled by KEEP_ALIVE_TIMER_VALUE + the time that has passed from the start of the
	 * KeepAlive timer to the time at which the message was sent. If the session was closed by the time this method
	 * starts to execute (the session state will become IDLE), that rescheduling won't occur.
	 */
	private synchronized void handleKeepaliveTimer() {
		final long ct = System.nanoTime();

		long nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());

		if (channel.isActive()) {
			if (ct >= nextKeepalive) {
				this.sendMessage(new PCEPKeepAliveMessage());
				nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(getKeepAliveTimerValue());
			}

			this.stateTimer.newTimeout(new TimerTask() {
				@Override
				public void run(final Timeout timeout) throws Exception {
					handleKeepaliveTimer();
				}
			}, nextKeepalive - ct, TimeUnit.NANOSECONDS);
		}
	}

	/**
	 * Sends message to serialization.
	 * 
	 * @param msg to be sent
	 */
	@Override
	public void sendMessage(final PCEPMessage msg) {
		try {
			this.channel.writeAndFlush(msg);
			this.lastMessageSentAt = System.nanoTime();
			if (!(msg instanceof PCEPKeepAliveMessage)) {
				logger.debug("Sent message: " + msg);
			}
			this.sentMsgCount++;
		} catch (final Exception e) {
			logger.warn("Message {} was not sent.", msg, e);
		}
	}

	/**
	 * Closes PCEP session without sending a Close message, as the channel is no longer active.
	 */
	@Override
	public void close() {
		logger.trace("Closing session: {}", this);
		this.channel.close();
	}

	/**
	 * Closes PCEP session, cancels all timers, returns to state Idle, sends the Close Message. KeepAlive and DeadTimer
	 * are cancelled if the state of the session changes to IDLE. This method is used to close the PCEP session from
	 * inside the session or from the listener, therefore the parent of this session should be informed.
	 */
	@Override
	public synchronized void close(final PCEPCloseObject.Reason reason) {
		logger.debug("Closing session: {}", this);
		this.closed = true;
		this.sendMessage(new PCEPCloseMessage(new PCEPCloseObject(reason)));
		this.channel.close();
	}

	private synchronized void terminate(final PCEPCloseObject.Reason reason) {
		this.listener.onSessionTerminated(this, new PCEPCloseTermination(reason));
		this.closed = true;
		this.sendMessage(new PCEPCloseMessage(new PCEPCloseObject(reason)));
		this.close();
	}

	@Override
	public synchronized void endOfInput() {
		if (!this.closed) {
			this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
			this.closed = true;
		}
	}

	private void sendErrorMessage(final PCEPErrors value) {
		this.sendErrorMessage(value, null);
	}

	/**
	 * Sends PCEP Error Message with one PCEPError and Open Object.
	 * 
	 * @param value
	 * @param open
	 */
	private void sendErrorMessage(final PCEPErrors value, final PCEPOpenObject open) {
		final PCEPErrorObject error = new PCEPErrorObject(value);
		final List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();
		errors.add(error);
		this.sendMessage(new PCEPErrorMessage(open, errors, null));
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
	public void handleMalformedMessage(final PCEPErrors error) {
		final long ct = System.nanoTime();
		this.sendErrorMessage(error);
		if (error == PCEPErrors.CAPABILITY_NOT_SUPPORTED) {
			this.unknownMessagesTimes.add(ct);
			while (ct - this.unknownMessagesTimes.peek() > 60 * 1E9) {
				this.unknownMessagesTimes.poll();
			}
			if (this.unknownMessagesTimes.size() > this.maxUnknownMessages) {
				this.terminate(Reason.TOO_MANY_UNKNOWN_MSG);
			}
		}
	}

	/**
	 * Handles incoming message. If the session is up, it notifies the user. The user is notified about every message
	 * except KeepAlive.
	 * 
	 * @param msg incoming message
	 */
	@Override
	public void handleMessage(final PCEPMessage msg) {
		// Update last reception time
		this.lastMessageReceivedAt = System.nanoTime();
		this.receivedMsgCount++;

		// Internal message handling. The user does not see these messages
		if (msg instanceof PCEPKeepAliveMessage) {
			// Do nothing, the timer has been already reset
		} else if (msg instanceof PCEPOpenMessage) {
			this.sendErrorMessage(PCEPErrors.ATTEMPT_2ND_SESSION);
		} else if (msg instanceof PCEPCloseMessage) {
			/*
			 * Session is up, we are reporting all messages to user. One notable
			 * exception is CLOSE message, which needs to be converted into a
			 * session DOWN event.
			 */
			this.close();
		} else {
			// This message needs to be handled by the user
			this.listener.onMessage(this, msg);
		}
	}

	/**
	 * @return the sentMsgCount
	 */

	@Override
	public Integer getSentMsgCount() {
		return this.sentMsgCount;
	}

	/**
	 * @return the receivedMsgCount
	 */

	@Override
	public Integer getReceivedMsgCount() {
		return this.receivedMsgCount;
	}

	@Override
	public Integer getDeadTimerValue() {
		return remoteOpen.getDeadTimerValue();
	}

	@Override
	public Integer getKeepAliveTimerValue() {
		return localOpen.getKeepAliveTimerValue();
	}

	@Override
	public String getPeerAddress() {
		InetSocketAddress a = (InetSocketAddress) channel.remoteAddress();
		return a.getHostName();
	}

	@Override
	public void tearDown() {
		this.close();
	}

	@Override
	public String getNodeIdentifier() {
		for (PCEPTlv tlv : this.remoteOpen.getTlvs()) {
			if (tlv instanceof NodeIdentifierTlv) {
				return tlv.toString();
			}
		}
		return "";
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("localOpen", localOpen);
		toStringHelper.add("remoteOpen", remoteOpen);
		return toStringHelper;
	}

	@Override
	protected void sessionUp() {
		listener.onSessionUp(this);
	}
}
