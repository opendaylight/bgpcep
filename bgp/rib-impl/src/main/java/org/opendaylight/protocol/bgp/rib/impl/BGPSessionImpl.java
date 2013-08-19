/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposalChecker;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessage;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ProtocolSessionOutboundHandler;
import org.opendaylight.protocol.framework.SessionParent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

class BGPSessionImpl implements BGPSession, ProtocolSession {

	private static final Logger logger = LoggerFactory.getLogger(BGPSessionImpl.class);

	/**
	 * KeepAlive Timer is to be scheduled periodically, each time it starts, it sends KeepAlive Message.
	 */
	private class KeepAliveTimer extends TimerTask {
		private final BGPSessionImpl parent;

		public KeepAliveTimer(final BGPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleKeepaliveTimer();
		}
	}

	/**
	 * HoldTimer is to be scheduled periodically, when it expires, it closes BGP session.
	 */
	private class HoldTimer extends TimerTask {
		private final BGPSessionImpl parent;

		public HoldTimer(final BGPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleHoldTimer();
		}
	}

	private static final int DEFAULT_HOLD_TIMER_VALUE = 15;

	public static int HOLD_TIMER_VALUE = DEFAULT_HOLD_TIMER_VALUE; // 240

	public int KEEP_ALIVE_TIMER_VALUE;

	/**
	 * Possible states for Finite State Machine
	 */
	private enum State {
		IDLE, OPEN_SENT, OPEN_CONFIRM, ESTABLISHED
	}

	/**
	 * Actual state of the FSM.
	 */
	private State state;

	/**
	 * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
	 */
	protected long lastMessageSentAt;

	/**
	 * System.nanoTime value about when was received the last message
	 */
	private long lastMessageReceivedAt;

	private final int sessionId;

	private final BGPSessionListener listener;

	/**
	 * Open message with session characteristics that were accepted by another BGP (sent from this session).
	 */
	private BGPSessionPreferences localOpen = null;

	/**
	 * Open Object with session characteristics for this session (sent from another BGP speaker).
	 */
	private BGPSessionPreferences remoteOpen = null;

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	private final SessionParent parent;

	private final ProtocolMessageFactory parser;

	private final BGPSessionProposalChecker checker;

	private final BGPSynchronization sync;

	private final ProtocolSessionOutboundHandler handler;

	private int kaCounter = 0;

	private final ChannelHandlerContext ctx;

	BGPSessionImpl(final SessionParent parent, final Timer timer, final BGPConnection connection, final int sessionId,
			final ProtocolMessageFactory parser, final ChannelHandlerContext ctx) {
		this.state = State.IDLE;
		this.listener = connection.getListener();
		this.sessionId = sessionId;
		this.localOpen = connection.getProposal();
		this.stateTimer = timer;
		this.parent = parent;
		this.parser = parser;
		this.ctx = ctx;
		this.checker = connection.getProposalChecker();
		this.sync = new BGPSynchronization(this.listener);
		this.handler = new ProtocolSessionOutboundHandler();
	}

	@Override
	public void close() {
		logger.debug("Closing session: " + this);
		if (this.state == State.ESTABLISHED) {
			this.sendMessage(new BGPNotificationMessage(BGPError.CEASE));
		}
		this.changeState(State.IDLE);
		this.parent.onSessionClosed(this);
	}

	@Override
	public void startSession() {
		logger.debug("Session started.");
		this.sendMessage(new BGPOpenMessage(this.localOpen.getMyAs(), (short) this.localOpen.getHoldTime(), this.localOpen.getBgpId(), this.localOpen.getParams()));
		this.stateTimer.schedule(new HoldTimer(this), DEFAULT_HOLD_TIMER_VALUE * 1000);
		this.changeState(State.OPEN_SENT);
	}

	/**
	 * Handles incoming message based on their type.
	 * 
	 * @param msg incoming message
	 */
	@Override
	public void handleMessage(final ProtocolMessage msg) {
		final BGPMessage bgpMsg = (BGPMessage) msg;
		// Update last reception time
		this.lastMessageReceivedAt = System.nanoTime();

		// Open messages are handled internally, but are parsed also in bgp-parser, so notify bgp listener
		if (bgpMsg instanceof BGPOpenMessage) {
			this.handleOpenMessage((BGPOpenMessage) bgpMsg);
		}
		// Keepalives are handled internally
		else if (bgpMsg instanceof BGPKeepAliveMessage) {
			this.handleKeepAliveMessage();
		}
		// Notifications are handled internally
		else if (bgpMsg instanceof BGPNotificationMessage) {
			logger.info("Session closed because Notification message received: {}" + ((BGPNotificationMessage) bgpMsg).getError());
			this.closeWithoutMessage();
			this.listener.onSessionTerminated(((BGPNotificationMessage) bgpMsg).getError());
		} else {
			this.listener.onMessage(bgpMsg);
		}
	}

	@Override
	public void handleMalformedMessage(final DeserializerException e) {
		logger.warn("Received malformed message: {}", e.getMessage(), e);
		this.terminate(BGPError.FSM_ERROR);
	}

	@Override
	public void handleMalformedMessage(final DocumentedException e) {
		logger.warn("Received malformed message: {}", e.getMessage(), e);
		this.terminate(((BGPDocumentedException) e).getError());
	}

	@Override
	public void endOfInput() {
		if (this.state != State.IDLE) {
			this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
		}
	}

	@Override
	public ProtocolMessageFactory getMessageFactory() {
		return this.parser;
	}

	@Override
	public int maximumMessageSize() {
		return 4096;
	}

	void sendMessage(final BGPMessage msg) {
		try {
			this.handler.writeDown(this.ctx, msg);
			this.lastMessageSentAt = System.nanoTime();
			logger.debug("Sent message: " + msg);
		} catch (final Exception e) {
			logger.warn("Message {} was not sent.", msg, e);
		}
	}

	private void closeWithoutMessage() {
		logger.debug("Closing session: " + this);
		HOLD_TIMER_VALUE = DEFAULT_HOLD_TIMER_VALUE;
		this.changeState(State.IDLE);
		this.parent.onSessionClosed(this);
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
		this.listener.onSessionTerminated(error);
	}

	/**
	 * If HoldTimer expires, the session ends. If a message (whichever) was received during this period, the HoldTimer
	 * will be rescheduled by HOLD_TIMER_VALUE + the time that has passed from the start of the HoldTimer to the time at
	 * which the message was received. If the session was closed by the time this method starts to execute (the session
	 * state will become IDLE), then rescheduling won't occur.
	 */
	private synchronized void handleHoldTimer() {
		final long ct = System.nanoTime();

		final long nextHold = (long) (this.lastMessageReceivedAt + (HOLD_TIMER_VALUE * 1E9));

		if (this.state != State.IDLE) {
			if (ct >= nextHold) {
				logger.debug("HoldTimer expired. " + new Date());
				this.terminate(BGPError.HOLD_TIMER_EXPIRED);
				return;
			}
			this.stateTimer.schedule(new HoldTimer(this), (long) ((nextHold - ct) / 1E6));
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

		long nextKeepalive = (long) (this.lastMessageSentAt + (this.KEEP_ALIVE_TIMER_VALUE * 1E9));

		if (this.state == State.ESTABLISHED) {
			if (ct >= nextKeepalive) {
				this.sendMessage(new BGPKeepAliveMessage());
				nextKeepalive = (long) (this.lastMessageSentAt + (this.KEEP_ALIVE_TIMER_VALUE * 1E9));
			}
			this.stateTimer.schedule(new KeepAliveTimer(this), (long) ((nextKeepalive - ct) / 1E6));
		}
	}

	private void changeState(final State finalState) {
		final String desc = "Changed to state: ";
		switch (finalState) {
		case IDLE:
			logger.debug(desc + State.IDLE);
			this.state = State.IDLE;
			return;
		case OPEN_SENT:
			logger.debug(desc + State.OPEN_SENT);
			if (this.state != State.IDLE) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.OPEN_SENT);
			}
			this.state = State.OPEN_SENT;
			return;
		case OPEN_CONFIRM:
			logger.debug(desc + State.OPEN_CONFIRM);
			if (this.state == State.ESTABLISHED) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.OPEN_CONFIRM);
			}
			this.state = State.OPEN_CONFIRM;
			return;
		case ESTABLISHED:
			logger.debug(desc + State.ESTABLISHED);
			if (this.state != State.OPEN_CONFIRM) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.ESTABLISHED);
			}
			this.state = State.ESTABLISHED;
			return;
		}
	}

	/**
	 * Open message should be handled only if the FSM is in OPEN_SENT or IDLE state. When in IDLE state, the Open
	 * message was received _before_ local Open message was sent. When in OPEN_SENT state, the message was received
	 * _after_ local Open message was sent.
	 * 
	 * @param msg received Open Message.
	 */
	private void handleOpenMessage(final BGPOpenMessage msg) {
		this.remoteOpen = new BGPSessionPreferences(msg.getMyAS(), msg.getHoldTime(), msg.getBgpId(), msg.getOptParams());
		logger.debug("Received message: {}", msg.toString());
		if (this.state != State.IDLE && this.state != State.OPEN_SENT) {
			this.terminate(BGPError.FSM_ERROR);
			return;
		}
		// if the session characteristics were unacceptable, the session is terminated
		// with given BGP error
		try {
			this.checker.checkSessionCharacteristics(this.remoteOpen);
		} catch (final BGPDocumentedException e) {
			this.terminate(e.getError());
		}
		// the session characteristics were acceptable
		HOLD_TIMER_VALUE = this.remoteOpen.getHoldTime();
		logger.debug("Session chars are acceptable. Overwriting: holdtimer: {}", HOLD_TIMER_VALUE);
		// when in IDLE state, we haven't send Open Message yet, do it now
		if (this.state == State.IDLE) {
			this.sendMessage(new BGPOpenMessage(this.localOpen.getMyAs(), (short) this.localOpen.getHoldTime(), this.localOpen.getBgpId(), this.localOpen.getParams()));
		}
		this.sendMessage(new BGPKeepAliveMessage());
		// if the timer is not disabled
		if (HOLD_TIMER_VALUE != 0) {
			this.KEEP_ALIVE_TIMER_VALUE = HOLD_TIMER_VALUE / 3;
			this.stateTimer.schedule(new KeepAliveTimer(this), this.KEEP_ALIVE_TIMER_VALUE * 1000);
			this.stateTimer.schedule(new HoldTimer(this), HOLD_TIMER_VALUE * 1000);
		}
		this.changeState(State.OPEN_CONFIRM);
	}

	/**
	 * KeepAlive message should be explicitly parsed in FSM when its state is OPEN_CONFIRM. Otherwise is handled by the
	 * KeepAliveTimer or it's invalid.
	 */
	private void handleKeepAliveMessage() {
		logger.debug("Received KeepAlive messsage.");
		if (this.state == State.OPEN_CONFIRM) {
			if (HOLD_TIMER_VALUE != 0) {
				this.stateTimer.schedule(new HoldTimer(this), HOLD_TIMER_VALUE * 1000);
				this.stateTimer.schedule(new KeepAliveTimer(this), this.KEEP_ALIVE_TIMER_VALUE * 1000);
			}
			this.changeState(State.ESTABLISHED);
			final Set<BGPTableType> tts = Sets.newHashSet();
			if (this.remoteOpen.getParams() != null) {
				for (final BGPParameter param : this.remoteOpen.getParams()) {
					if (param instanceof MultiprotocolCapability) {
						tts.add(((MultiprotocolCapability) param).getTableType());
					}
				}
			}
			this.sync.addTableTypes(tts);
			this.listener.onSessionUp(tts);
			// check if the KA is EOR for some AFI/SAFI
		} else if (this.state == State.ESTABLISHED) {
			this.kaCounter++;
			if (this.kaCounter >= 2) {
				this.sync.kaReceived();
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPSessionImpl [state=");
		builder.append(this.state);
		builder.append(", sessionId=");
		builder.append(this.sessionId);
		builder.append(", localOpen=");
		builder.append(this.localOpen);
		builder.append(", remoteOpen=");
		builder.append(this.remoteOpen);
		builder.append("]");
		return builder.toString();
	}
}
