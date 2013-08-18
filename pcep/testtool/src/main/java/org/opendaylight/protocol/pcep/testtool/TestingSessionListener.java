/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import groovy.lang.GroovyClassLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.framework.TerminationReason;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.message.PCEPXRAddTunnelMessage;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestingSessionListener extends PCEPSessionListener {

	public List<PCEPMessage> messages = new ArrayList<PCEPMessage>();

	private static final Logger logger = LoggerFactory.getLogger(TestingSessionListener.class);

	public final Queue<PCEPMessage> replyMessages;
	public final Queue<PCEPMessage> sendNowMessages;
	public final Queue<PCEPMessage> periodicalMessages;
	public final int period;

	private class MessageTimer extends TimerTask {
		private final PCEPMessage message;
		private final PCEPSession session;

		MessageTimer(final PCEPMessage message, final PCEPSession session) {
			this.message = message;
			this.session = session;
		}

		@Override
		public void run() {
			this.session.sendMessage(this.message);
		}
	}

	public TestingSessionListener() {
		/**
		 * default messages are set to null
		 */
		this.period = 0;
		this.replyMessages = new LinkedList<PCEPMessage>();
		this.sendNowMessages = new LinkedList<PCEPMessage>();
		this.periodicalMessages = new LinkedList<PCEPMessage>();
	}

	public TestingSessionListener(final String autoResponseMessagesSrc, final String periodicallySendMessagesSrc, final int period,
			final String sendNowMessageSrc) {
		this.period = period;
		this.replyMessages = this.loadMessageGeneratorFromFile(autoResponseMessagesSrc).generateMessages();
		this.sendNowMessages = this.loadMessageGeneratorFromFile(sendNowMessageSrc).generateMessages();
		this.periodicalMessages = this.loadMessageGeneratorFromFile(periodicallySendMessagesSrc).generateMessages();
	}

	/**
	 * Periodically send messages from loaded queue.
	 * 
	 * @param session the session used to send message
	 * @param period the time between two sent messages
	 */
	public void sendPeriodically(final PCEPSession session, final int seconds) {
		if (!this.periodicalMessages.isEmpty()) {
			this.sendPeriodically(session, this.periodicalMessages.poll(), seconds);
		} else
			logger.debug("DON'T starting PERIODICAL sender. Messages queue is empty.");
	}

	/**
	 * Periodically send specified message.
	 * 
	 * @param session the session used to send message
	 * @param message the message to send
	 * @param period the time between two sent messages
	 */
	public void sendPeriodically(final PCEPSession session, final PCEPMessage message, final int seconds) {
		final Timer timer = new Timer();

		logger.debug("Starting periodical sending of messages with {} seconds delay.", seconds);
		timer.schedule(new MessageTimer(message, session), seconds * 1000);
	}

	public void sendNow(final PCEPSession session) {
		if (!this.sendNowMessages.isEmpty()) {
			final PCEPMessage msg = this.sendNowMessages.poll();
			this.sendNow(session, msg);
		} else
			logger.debug("DON'T sending NOW. Messages queue is empty.");
	}

	public void sendNow(final PCEPSession session, final PCEPMessage message) {
		logger.debug("Sending NOW.");
		session.sendMessage(message);
	}

	@Override
	public void onMessage(final PCEPSession session, final PCEPMessage message) {
		logger.debug("Received message: {}", message);
		this.messages.add(message);

		// if (!this.replyMessages.isEmpty()) {
		// this.logger.debug("Remaining messages in queue: {}", this.replyMessages.size());
		// session.sendMessage(this.replyMessages.poll());
		// } else
		// this.logger.debug("Reply messages queue is empty.");
	}

	@Override
	public void onSessionUp(final PCEPSession session, final PCEPOpenObject local, final PCEPOpenObject remote) {
		logger.debug("Session up.");
		final List<ExplicitRouteSubobject> subs = new ArrayList<ExplicitRouteSubobject>();
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 10, 1, 1, 2 }), 32), false));
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 2, 2, 2, 2 }), 32), false));
		session.sendMessage(new PCEPXRAddTunnelMessage(new PCEPLspObject(23, false, false, false, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(new byte[] {
				1, 1, 1, 1 }), new IPv4Address(new byte[] { 2, 2, 2, 2 })), new PCEPExplicitRouteObject(subs, false)));
		this.sendNow(session);
		this.sendPeriodically(session, this.period);
	}

	@Override
	public void onSessionDown(final PCEPSession session, final PCEPCloseObject reason, final Exception e) {
		logger.debug("Session down because: {}", reason);
	}

	@Override
	public void onSessionTerminated(final PCEPSession session, final TerminationReason cause) {
		logger.debug("Session terminated. Cause : " + cause.toString());
	}

	private MessageGeneratorService loadMessageGeneratorFromFile(final String path) {
		try {
			final GroovyClassLoader l = new GroovyClassLoader();
			final Class<?> scriptClass = l.parseClass(new File(path));
			l.close();
			logger.debug("Loaded '{}'", path);
			final MessageGeneratorService generator = (MessageGeneratorService) scriptClass.newInstance();
			logger.debug("Instantiated '{}'", path);
			return generator;
		} catch (final Exception e) {
			logger.error("Failed to load '{}'. Using empty queue.", path);
			return new MessageGeneratorService() {

				@Override
				public Queue<PCEPMessage> generateMessages() {
					return new LinkedList<PCEPMessage>();
				}
			};
		}

	}

	@SuppressWarnings("unused")
	private MessageGeneratorService loadMessageGeneratorFormSysResource(final String path) {
		try {
			final InputStream is = this.getClass().getResourceAsStream(path);
			final BufferedReader buffReader = new BufferedReader(new InputStreamReader(is));
			final StringBuilder script = new StringBuilder();
			String scriptLine;
			while ((scriptLine = buffReader.readLine()) != null) {
				script.append(scriptLine + "\n");
			}
			final GroovyClassLoader l = new GroovyClassLoader();
			final Class<?> scriptClass = l.parseClass(script.toString());
			l.close();
			logger.debug("Loaded '{}'", path);
			final MessageGeneratorService generator = (MessageGeneratorService) scriptClass.newInstance();
			logger.debug("Instantiated '{}'", path);
			return generator;

		} catch (final Exception e) {
			logger.error("Failed to load '{}' from system resources. Using empty queue.", path);
			return new MessageGeneratorService() {

				@Override
				public Queue<PCEPMessage> generateMessages() {
					return new LinkedList<PCEPMessage>();
				}
			};
		}
	}
}
