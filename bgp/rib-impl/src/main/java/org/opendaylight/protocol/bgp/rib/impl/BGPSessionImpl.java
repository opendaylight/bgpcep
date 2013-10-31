/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

@VisibleForTesting
public class BGPSessionImpl extends AbstractProtocolSession<BGPMessage> implements BGPSession {

	private static final Logger logger = LoggerFactory.getLogger(BGPSessionImpl.class);

	private static final int DEFAULT_HOLD_TIMER_VALUE = 15;

	public static int HOLD_TIMER_VALUE = DEFAULT_HOLD_TIMER_VALUE; // 240

	/**
	 * Internal session state.
	 */
	public enum State {
		/**
		 * The session object is created by the negotiator in OpenConfirm state.
		 * While in this state, the session object is half-alive, e.g. the timers
		 * are running, but the session is not completely up, e.g. it has not been
		 * announced to the listener. If the session is torn down in this state,
		 * we do not inform the listener.
		 */
		OpenConfirm,
		/**
		 * The session has been completely established.
		 */
		Up,
		/**
		 * The session has been closed. It will not be resurrected.
		 */
		Idle,
	}

	/**
	 * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
	 */
	protected long lastMessageSentAt;

	/**
	 * System.nanoTime value about when was received the last message
	 */
	private long lastMessageReceivedAt;

	private final BGPSessionListener listener;

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	private final BGPSynchronization sync;

	private int kaCounter = 0;

	private final Channel channel;

	@GuardedBy("this")
	private State state = State.OpenConfirm;

	private final int keepAlive;

	private final Set<BGPTableType> tableTypes;

	BGPSessionImpl(final Timer timer, final BGPSessionListener listener, final Channel channel, final BGPOpenMessage remoteOpen) {
		this.listener = Preconditions.checkNotNull(listener);
		this.stateTimer = Preconditions.checkNotNull(timer);
		this.channel = Preconditions.checkNotNull(channel);
		this.keepAlive = remoteOpen.getHoldTime() / 3;
		HOLD_TIMER_VALUE = remoteOpen.getHoldTime();

		final Set<BGPTableType> tts = Sets.newHashSet();
		if (remoteOpen.getOptParams() != null) {
			for (final BGPParameter param : remoteOpen.getOptParams()) {
				if (param instanceof MultiprotocolCapability) {
					tts.add(((MultiprotocolCapability) param).getTableType());
				}
			}
		}

		this.sync = new BGPSynchronization(this, this.listener, tts);
		this.tableTypes = tts;

		if (remoteOpen.getHoldTime() != 0) {
			this.stateTimer.newTimeout(new TimerTask() {

				@Override
				public void run(final Timeout timeout) throws Exception {
					handleHoldTimer();
				}
			}, remoteOpen.getHoldTime(), TimeUnit.SECONDS);

			this.stateTimer.newTimeout(new TimerTask() {
				@Override
				public void run(final Timeout timeout) throws Exception {
					handleKeepaliveTimer();
				}
			}, this.keepAlive, TimeUnit.SECONDS);
		}
	}

	@Override
	public synchronized void close() {
		logger.debug("Closing session: {}", this);
		if (this.state != State.Idle) {
			this.sendMessage(new BGPNotificationMessage(BGPError.CEASE));
			this.channel.close();
			this.state = State.Idle;
		}
	}

	/**
	 * Handles incoming message based on their type.
	 * 
	 * @param msg incoming message
	 */
	@Override
	public void handleMessage(final BGPMessage msg) {
		// Update last reception time
		this.lastMessageReceivedAt = System.nanoTime();

		if (msg instanceof BGPOpenMessage) {
			// Open messages should not be present here
			this.terminate(BGPError.FSM_ERROR);
		} else if (msg instanceof BGPNotificationMessage) {
			// Notifications are handled internally
			logger.info("Session closed because Notification message received: {}", ((BGPNotificationMessage) msg).getError());
			this.closeWithoutMessage();
			this.listener.onSessionTerminated(this, new BGPTerminationReason(((BGPNotificationMessage) msg).getError()));
		} else if (msg instanceof BGPKeepAliveMessage) {
			// Keepalives are handled internally
			logger.debug("Received KeepAlive messsage.");
			this.kaCounter++;
			if (this.kaCounter >= 2) {
				this.sync.kaReceived();
			}
		} else {
			// All others are passed up
			this.listener.onMessage(this, msg);
		}
	}

	@Override
	public synchronized void endOfInput() {
		if (this.state == State.Up) {
			this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
		}
	}

	void sendMessage(final BGPMessage msg) {
		try {
			this.channel.writeAndFlush(msg);
			this.lastMessageSentAt = System.nanoTime();
			logger.debug("Sent message: {}", msg);
		} catch (final Exception e) {
			logger.warn("Message {} was not sent.", msg, e);
		}
	}

	private synchronized void closeWithoutMessage() {
		logger.debug("Closing session: {}", this);
		this.channel.close();
		this.state = State.Idle;
	}

	/**
	 * Closes PCEP session from the parent with given reason. A message needs to be sent, but parent doesn't have to be
	 * modified, because he initiated the closing. (To prevent concurrent modification exception).
	 * 
	 * @param closeObject
	 */
	private void terminate(final BGPError error) {
		this.sendMessage(new BGPNotificationMessage(error));
		this.closeWithoutMessage();

		this.listener.onSessionTerminated(this, new BGPTerminationReason(error));
	}

	/**
	 * If HoldTimer expires, the session ends. If a message (whichever) was received during this period, the HoldTimer
	 * will be rescheduled by HOLD_TIMER_VALUE + the time that has passed from the start of the HoldTimer to the time at
	 * which the message was received. If the session was closed by the time this method starts to execute (the session
	 * state will become IDLE), then rescheduling won't occur.
	 */
	private synchronized void handleHoldTimer() {
		if (this.state == State.Idle) {
			return;
		}

		final long ct = System.nanoTime();
		final long nextHold = this.lastMessageReceivedAt + TimeUnit.SECONDS.toNanos(HOLD_TIMER_VALUE);

		if (ct >= nextHold) {
			logger.debug("HoldTimer expired. " + new Date());
			this.terminate(BGPError.HOLD_TIMER_EXPIRED);
		} else {
			this.stateTimer.newTimeout(new TimerTask() {
				@Override
				public void run(final Timeout timeout) throws Exception {
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
		if (this.state == State.Idle) {
			return;
		}

		final long ct = System.nanoTime();
		long nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(this.keepAlive);

		if (ct >= nextKeepalive) {
			this.sendMessage(new BGPKeepAliveMessage());
			nextKeepalive = this.lastMessageSentAt + TimeUnit.SECONDS.toNanos(this.keepAlive);
		}
		this.stateTimer.newTimeout(new TimerTask() {
			@Override
			public void run(final Timeout timeout) throws Exception {
				handleKeepaliveTimer();
			}
		}, nextKeepalive - ct, TimeUnit.NANOSECONDS);
	}

	@Override
	final public String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("channel", this.channel);
		toStringHelper.add("state", this.state);
		return toStringHelper;
	}

	@Override
	public Set<BGPTableType> getAdvertisedTableTypes() {
		return this.tableTypes;
	}

	@Override
	protected synchronized void sessionUp() {
		this.state = State.Up;
		this.listener.onSessionUp(this);
	}

	public synchronized State getState() {
		return this.state;
	}
}
