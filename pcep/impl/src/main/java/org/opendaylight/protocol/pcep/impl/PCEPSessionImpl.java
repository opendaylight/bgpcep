/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessage;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.SessionParent;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPErrorTermination;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.message.PCEPRawMessage;
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

/**
 * Implementation of PCEPSession. (Not final for testing.)
 */
class PCEPSessionImpl implements PCEPSession, ProtocolSession, PCEPSessionRuntimeMXBean {

	/**
	 * KeepAlive Timer is to be scheduled periodically, each time it starts, it sends KeepAlive Message.
	 */
	private class KeepAliveTimer extends TimerTask {
		private final PCEPSessionImpl parent;

		public KeepAliveTimer(final PCEPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleKeepaliveTimer();
		}
	}

	/**
	 * DeadTimer is to be scheduled periodically, when it expires, it closes PCEP session.
	 */
	private class DeadTimer extends TimerTask {
		private final PCEPSessionImpl parent;

		public DeadTimer(final PCEPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleDeadtimer();
		}
	}

	/**
	 * OpenWaitTimer runs just once, but can be rescheduled or canceled before expiration. When it expires, it sends an
	 * error message (1, 2)
	 */
	private class OpenWaitTimer extends TimerTask {

		private final PCEPSessionImpl parent;

		public OpenWaitTimer(final PCEPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleOpenWait();
		}
	}

	/**
	 * KeepWaitTimer runs just once, but can be rescheduled or canceled before expiration. When it expires, it sends an
	 * error message (1, 7)
	 */
	private class KeepWaitTimer extends TimerTask {

		private final PCEPSessionImpl parent;

		public KeepWaitTimer(final PCEPSessionImpl parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			this.parent.handleKeepWait();
		}
	}

	/**
	 * Possible states for Finite State Machine
	 */
	private enum State {
		IDLE, OPEN_WAIT, KEEP_WAIT, UP
	}

	/**
	 * In seconds.
	 */
	public static final int OPEN_WAIT_TIMER_VALUE = 60;

	public static final int KEEP_WAIT_TIMER_VALUE = 60;

	public int KEEP_ALIVE_TIMER_VALUE = 3;

	public int DEAD_TIMER_VALUE = 4 * this.KEEP_ALIVE_TIMER_VALUE;

	/**
	 * Actual state of the FSM.
	 */
	private State state;

	private OpenWaitTimer openWaitTimer;

	private KeepWaitTimer keepWaitTimer;

	/**
	 * System.nanoTime value about when was sent the last message Protected to be updated also in tests.
	 */
	protected long lastMessageSentAt;

	/**
	 * System.nanoTime value about when was received the last message
	 */
	private long lastMessageReceivedAt;

	private boolean localOK = false;

	private boolean remoteOK = false;

	private boolean openRetry = false;

	private final int sessionId;

	/**
	 * Protected for testing.
	 */
	protected int maxUnknownMessages = 5;

	protected final Queue<Long> unknownMessagesTimes = new LinkedList<Long>();

	private final PCEPSessionListener listener;

	private PCEPSessionProposalChecker checker = null;

	/**
	 * Open Object with session characteristics that were accepted by another PCE (sent from this session).
	 */
	private PCEPOpenObject localOpen = null;

	/**
	 * Open Object with session characteristics for this session (sent from another PCE).
	 */
	private PCEPOpenObject remoteOpen = null;

	private static final Logger logger = LoggerFactory.getLogger(PCEPSessionImpl.class);

	/**
	 * Timer object grouping FSM Timers
	 */
	private final Timer stateTimer;

	private final SessionParent parent;

	private final PCEPMessageFactory factory;

	private int sentMsgCount = 0;

	private int receivedMsgCount = 0;

	private final String peerAddress;

	private final ChannelHandlerContext ctx;

	PCEPSessionImpl(final SessionParent parent, final Timer timer, final PCEPConnection connection, final PCEPMessageFactory factory,
			final int maxUnknownMessages, final int sessionId, final ChannelHandlerContext ctx) {
		this.state = State.IDLE;
		this.listener = connection.getListener();
		this.checker = connection.getProposalChecker();
		this.sessionId = sessionId;
		this.localOpen = connection.getProposal().getOpenObject();
		this.peerAddress = connection.getPeerAddress().getHostString();
		this.stateTimer = timer;
		this.parent = parent;
		this.factory = factory;
		this.ctx = ctx;
		if (this.maxUnknownMessages != 0)
			this.maxUnknownMessages = maxUnknownMessages;
	}

	@Override
	public void startSession() {
		logger.debug("Session started.");
		this.sendMessage(new PCEPOpenMessage(this.localOpen));
		this.restartOpenWait();
		this.changeState(State.OPEN_WAIT);
	}

	/**
	 * OpenWait timer can be canceled or rescheduled before its expiration. When it expires, it sends particular
	 * PCEPErrorMessage and closes PCEP session.
	 */
	private synchronized void handleOpenWait() {
		if (this.state != State.IDLE) {
			this.terminate(PCEPErrors.NO_OPEN_BEFORE_EXP_OPENWAIT); // 1, 1
		}
	}

	/**
	 * KeepWait timer can be canceled or rescheduled before its expiration. When it expires, it sends particular
	 * PCEPErrorMessage and closes PCEP session.
	 */
	private synchronized void handleKeepWait() {
		if (this.state != State.IDLE) {
			this.terminate(PCEPErrors.NO_MSG_BEFORE_EXP_KEEPWAIT); // 1, 7
		}
	}

	/**
	 * If DeadTimer expires, the session ends. If a message (whichever) was received during this period, the DeadTimer
	 * will be rescheduled by DEAD_TIMER_VALUE + the time that has passed from the start of the DeadTimer to the time at
	 * which the message was received. If the session was closed by the time this method starts to execute (the session
	 * state will become IDLE), that rescheduling won't occur.
	 */
	private synchronized void handleDeadtimer() {
		final long ct = System.nanoTime();

		final long nextDead = (long) (this.lastMessageReceivedAt + (this.DEAD_TIMER_VALUE * 1E9));

		if (this.state != State.IDLE) {
			if (ct >= nextDead) {
				logger.debug("DeadTimer expired. " + new Date());
				this.terminate(Reason.EXP_DEADTIMER);
				return;
			}

			this.stateTimer.schedule(new DeadTimer(this), (long) ((nextDead - ct) / 1E6));
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

		if (this.state != State.IDLE) {
			if (ct >= nextKeepalive) {
				this.sendMessage(new PCEPKeepAliveMessage());
				nextKeepalive = (long) (this.lastMessageSentAt + (this.KEEP_ALIVE_TIMER_VALUE * 1E9));
			}

			this.stateTimer.schedule(new KeepAliveTimer(this), (long) ((nextKeepalive - ct) / 1E6));
		}
	}

	private void changeState(final State finalState) {
		switch (finalState) {
		case IDLE:
			logger.debug("Changed to state: " + State.IDLE);
			this.state = State.IDLE;
			return;
		case OPEN_WAIT:
			logger.debug("Changed to state: " + State.OPEN_WAIT);
			if (this.state == State.UP) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.OPEN_WAIT);
			}
			this.state = State.OPEN_WAIT;
			return;
		case KEEP_WAIT:
			logger.debug("Changed to state: " + State.KEEP_WAIT);
			if (this.state == State.UP || this.state == State.IDLE) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.KEEP_WAIT);
			}
			this.state = State.KEEP_WAIT;
			return;
		case UP:
			logger.debug("Changed to state: " + State.UP);
			if (this.state == State.IDLE || this.state == State.UP) {
				throw new IllegalArgumentException("Cannot change state from " + this.state + " to " + State.UP);
			}
			this.state = State.UP;
			return;
		}
	}

	private void restartOpenWait() {
		if (this.state == State.OPEN_WAIT && this.openWaitTimer != null) {
			this.openWaitTimer.cancel();
		}
		this.openWaitTimer = new OpenWaitTimer(this);
		this.stateTimer.schedule(this.openWaitTimer, OPEN_WAIT_TIMER_VALUE * 1000);
	}

	private void restartKeepWaitTimer() {
		if (this.state == State.KEEP_WAIT && this.keepWaitTimer != null) {
			this.keepWaitTimer.cancel();
		}
		this.keepWaitTimer = new KeepWaitTimer(this);
		this.stateTimer.schedule(this.keepWaitTimer, KEEP_WAIT_TIMER_VALUE * 1000);
	}

	/**
	 * Makes a callback to check if the session characteristics that FSM received, are acceptable.
	 * 
	 * @param keepAliveTimerValue
	 * @param deadTimerValue
	 * @param tlvs
	 * @return
	 */
	private boolean checkSessionCharacteristics(final PCEPOpenObject openObj) {
		return this.checker.checkSessionCharacteristics(new PCEPSessionPreferences(openObj));
	}

	private PCEPOpenObject getNewProposal() {
		return this.checker.getNewProposal(new PCEPSessionPreferences(this.localOpen)).getOpenObject();
	}

	/**
	 * Sends message to serialization.
	 * 
	 * @param msg to be sent
	 */
	@Override
	public void sendMessage(final PCEPMessage msg) {
		try {
			this.ctx.writeAndFlush(msg);
			this.lastMessageSentAt = System.nanoTime();
			if (!(msg instanceof PCEPKeepAliveMessage))
				logger.debug("Sent message: " + msg);
			this.sentMsgCount++;
		} catch (final Exception e) {
			logger.warn("Message {} was not sent.", msg, e);
		}
	}

	/**
	 * Closes PCEP session without sending a Close message, as the channel is no longer active. Notify parent about
	 * this.
	 * 
	 * @param reason reason, why it was terminated
	 */
	@Override
	public void close() {
		logger.trace("Closing session: {}", this);
		this.changeState(State.IDLE);
		this.parent.onSessionClosed(this);
	}

	/**
	 * Closes PCEP session, cancels all timers, returns to state Idle, sends the Close Message. KeepAlive and DeadTimer
	 * are cancelled if the state of the session changes to IDLE. This method is used to close the PCEP session from
	 * inside the session or from the listener, therefore the parent of this session should be informed.
	 */
	@Override
	public synchronized void close(final PCEPCloseObject.Reason reason) {
		logger.debug("Closing session: {}", this);
		this.sendMessage(new PCEPCloseMessage(new PCEPCloseObject(reason)));
		this.changeState(State.IDLE);
		this.parent.onSessionClosed(this);
	}

	private void terminate(final PCEPCloseObject.Reason reason) {
		this.listener.onSessionTerminated(this, new PCEPCloseTermination(reason));
		this.sendMessage(new PCEPCloseMessage(new PCEPCloseObject(reason)));
		this.close();
	}

	private void terminate(final PCEPErrors error) {
		this.listener.onSessionTerminated(this, new PCEPErrorTermination(error));
		this.sendErrorMessage(error);
		this.close();
	}

	@Override
	public void endOfInput() {
		if (this.state != State.IDLE) {
			this.listener.onSessionDown(this, null, new IOException("End of input detected. Close the session."));
		}
	}

	@Override
	public int maximumMessageSize() {
		return 65535;
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

	@Override
	public void handleMalformedMessage(final DeserializerException e) {
		// FIXME rewrite

	}

	@Override
	public void handleMalformedMessage(final DocumentedException e) {
		// FIXME rewrite

	}

	/**
	 * The fact, that a message is malformed, comes from parser. In case of unrecognized message a particular error is
	 * sent (CAPABILITY_NOT_SUPPORTED) and the method checks if the MAX_UNKNOWN_MSG per minute wasn't overstepped.
	 * Second, any other error occurred that is specified by rfc. In this case, the an error message is generated and
	 * sent.
	 * 
	 * @param error documented error in RFC5440 or draft
	 */
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
	 * In case of syntactic error or some parsing error, the session needs to be closed with the Reason: malformed
	 * message. The user needs to be notified about this error.
	 * 
	 * @param e exception that was thrown from parser
	 */
	public void handleMalformedMessage(final Exception e) {
		logger.warn("PCEP byte stream corruption detected", e);
		this.terminate(Reason.MALFORMED_MSG);
	}

	/**
	 * Open message should be handled only if the FSM is in OPEN_WAIT state.
	 * 
	 * @param msg
	 */
	private void handleOpenMessage(final PCEPOpenMessage msg) {
		this.remoteOpen = msg.getOpenObject();
		logger.debug("Received message: " + msg.toString());
		if (this.state != State.OPEN_WAIT) {
			this.sendErrorMessage(PCEPErrors.ATTEMPT_2ND_SESSION);
			return;
		}
		final Boolean result = this.checkSessionCharacteristics(this.remoteOpen);
		if (result == null) {
			this.terminate(PCEPErrors.NON_ACC_NON_NEG_SESSION_CHAR); // 1, 3
			return;
		} else if (result) {
			this.DEAD_TIMER_VALUE = this.remoteOpen.getDeadTimerValue();
			this.KEEP_ALIVE_TIMER_VALUE = this.remoteOpen.getKeepAliveTimerValue();
			logger.debug("Session chars are acceptable. Overwriting: deadtimer: " + this.DEAD_TIMER_VALUE + "keepalive: "
					+ this.KEEP_ALIVE_TIMER_VALUE);
			this.remoteOK = true;
			this.openWaitTimer.cancel();
			this.sendMessage(new PCEPKeepAliveMessage());
			// if the timer is not disabled
			if (this.KEEP_ALIVE_TIMER_VALUE != 0) {
				this.stateTimer.schedule(new KeepAliveTimer(this), this.KEEP_ALIVE_TIMER_VALUE * 1000);
			}
			if (this.localOK) {
				// if the timer is not disabled
				if (this.DEAD_TIMER_VALUE != 0) {
					this.stateTimer.schedule(new DeadTimer(this), this.DEAD_TIMER_VALUE * 1000);
				}
				this.changeState(State.UP);
				this.listener.onSessionUp(this, this.localOpen, this.remoteOpen);
			} else {
				this.restartKeepWaitTimer();
				this.changeState(State.KEEP_WAIT);
			}
			return;
		} else if (!result) {
			this.localOpen = this.getNewProposal();
			if (this.openRetry) {
				this.terminate(PCEPErrors.SECOND_OPEN_MSG); // 1, 5
			} else {
				this.openRetry = true;
				this.sendErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR, this.localOpen); // 1, 4
				if (this.localOK) {
					this.restartOpenWait();
					this.changeState(State.OPEN_WAIT);
				} else {
					this.restartKeepWaitTimer();
					this.changeState(State.KEEP_WAIT);
				}
			}
		}
	}

	/**
	 * Error message should be handled in FSM if its state is KEEP_WAIT, otherwise it is just passed to session listener
	 * for handling.
	 * 
	 * @param msg
	 */
	private void handleErrorMessage(final PCEPErrorMessage msg) {
		this.remoteOpen = msg.getOpenObject();
		final Boolean result = this.checkSessionCharacteristics(this.remoteOpen);
		if (result == null || !result) {
			this.terminate(PCEPErrors.PCERR_NON_ACC_SESSION_CHAR); // 1, 6
			return;
		} else {
			this.KEEP_ALIVE_TIMER_VALUE = this.remoteOpen.getKeepAliveTimerValue();
			this.DEAD_TIMER_VALUE = this.remoteOpen.getDeadTimerValue();
			logger.debug("New values for keepalive: " + this.remoteOpen.getKeepAliveTimerValue() + " deadtimer "
					+ this.remoteOpen.getDeadTimerValue());
			this.sendMessage(new PCEPOpenMessage(this.remoteOpen));
			if (this.remoteOK) {
				this.restartKeepWaitTimer();
				this.changeState(State.KEEP_WAIT);
			} else {
				this.keepWaitTimer.cancel();
				this.restartOpenWait();
				this.changeState(State.OPEN_WAIT);
			}
		}
	}

	/**
	 * KeepAlive message should be explicitly parsed in FSM when its state is KEEP_WAIT. Otherwise is handled by the
	 * KeepAliveTimer or it's invalid.
	 */
	private void handleKeepAliveMessage() {
		if (this.state == State.KEEP_WAIT) {
			this.localOK = true;
			this.keepWaitTimer.cancel();
			if (this.remoteOK) {
				if (this.DEAD_TIMER_VALUE != 0) {
					this.stateTimer.schedule(new DeadTimer(this), this.DEAD_TIMER_VALUE * 1000);
				}
				this.changeState(State.UP);
				this.listener.onSessionUp(this, this.localOpen, this.remoteOpen);
			} else {
				this.restartOpenWait();
				this.changeState(State.OPEN_WAIT);
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
	public void handleMessage(final ProtocolMessage msg) {
		// Update last reception time
		final PCEPMessage pcepMsg = (PCEPMessage) msg;

		this.lastMessageReceivedAt = System.nanoTime();
		this.receivedMsgCount++;

		if (pcepMsg instanceof PCEPRawMessage) {
			List<PCEPMessage> msgs;
			try {
				msgs = PCEPMessageValidator.getValidator(((PCEPRawMessage) pcepMsg).getMsgType()).validate(
						((PCEPRawMessage) pcepMsg).getAllObjects());
				for (final PCEPMessage tmpMsg : msgs) {
					this.handleMessage(tmpMsg);
				}
			} catch (final PCEPDeserializerException e) {
				logger.error("Malformed message, terminating. ", e);
				this.terminate(Reason.MALFORMED_MSG);
			}
			return;
		}

		// Keepalives are handled internally
		if (pcepMsg instanceof PCEPKeepAliveMessage) {
			this.handleKeepAliveMessage();
			return;
		}

		// Open messages are handled internally
		if (pcepMsg instanceof PCEPOpenMessage) {
			this.handleOpenMessage((PCEPOpenMessage) pcepMsg);
			return;
		}

		/*
		 * During initial handshake we handle all the messages.
		 */
		if (this.state != State.UP) {
			/*
			 * In KEEP_WAIT, an Error message is a valid thing to see, because
			 * it is used in negotiation.
			 */
			if (pcepMsg instanceof PCEPErrorMessage && this.state == State.KEEP_WAIT
					&& ((PCEPErrorMessage) pcepMsg).getOpenObject() != null) {
				this.handleErrorMessage((PCEPErrorMessage) pcepMsg);
				return;
			}

			/*
			 * OPEN and KEEPALIVE messages are handled at the top. ERROR
			 * messages are handled in the specific case of KEEP_WAIT above, so
			 * anything else is invalid here.
			 */
			this.terminate(PCEPErrors.NON_OR_INVALID_OPEN_MSG);
			return;
		}

		/*
		 * Session is up, we are reporting all messages to user. One notable
		 * exception is CLOSE message, which needs to be converted into a
		 * session DOWN event.
		 */
		if (pcepMsg instanceof PCEPCloseMessage) {
			this.close();
			return;
		}
		this.listener.onMessage(this, pcepMsg);
	}

	@Override
	public ProtocolMessageFactory getMessageFactory() {
		return this.factory;
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
		return this.DEAD_TIMER_VALUE;
	}

	@Override
	public Integer getKeepAliveTimerValue() {
		return this.KEEP_ALIVE_TIMER_VALUE;
	}

	@Override
	public String getPeerAddress() {
		return this.peerAddress;
	}

	@Override
	public void tearDown() throws IOException {
		this.close();
	}

	@Override
	public String getNodeIdentifier() {
		if (!this.remoteOpen.getTlvs().isEmpty()) {
			final PCEPTlv tlv = this.remoteOpen.getTlvs().iterator().next();
			if (tlv != null && tlv instanceof NodeIdentifierTlv) {
				return tlv.toString();
			}
		}
		return "";
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPSessionImpl [state=");
		builder.append(this.state);
		builder.append(", localOK=");
		builder.append(this.localOK);
		builder.append(", remoteOK=");
		builder.append(this.remoteOK);
		builder.append(", openRetry=");
		builder.append(this.openRetry);
		builder.append(", sessionId=");
		builder.append(this.sessionId);
		builder.append(", checker=");
		builder.append(this.checker);
		builder.append(", localOpen=");
		builder.append(this.localOpen);
		builder.append(", remoteOpen=");
		builder.append(this.remoteOpen);
		builder.append("]");
		return builder.toString();
	}
}
