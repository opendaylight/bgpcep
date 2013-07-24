/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Session Listener that is notified about messages and changes in the session.
 */
public class SimpleSessionListener implements SessionListener {
	private static final Logger logger = LoggerFactory.getLogger(SimpleSessionListener.class);

	public List<ProtocolMessage> messages = new ArrayList<ProtocolMessage>();

	public boolean up = false;

	public boolean failed = false;

	public void onMessage(ProtocolSession session, ProtocolMessage message) {
		logger.debug("Received message: " + message.getClass() + " " + message);
		this.messages.add(message);
	}

	public synchronized void onSessionUp(ProtocolSession session, SimpleSessionPreferences local,
			SimpleSessionPreferences remote) {
		logger.debug("Session up.");
		this.up = true;
		this.notifyAll();
	}

	public synchronized void onConnectionFailed(ProtocolSession session, Exception e) {
		logger.debug("Connection Failed: {}", e.getMessage(), e);
		this.failed = true;
		this.notifyAll();
		try {
			session.close();
		} catch (final IOException ex) {
			logger.warn("Session could not be closed.");
		}
	}
}
