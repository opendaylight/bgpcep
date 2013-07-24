/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.framework.TerminationReason;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 * Simple Session Listener that is notified about messages and changes in the session.
 */
public class SimpleSessionListener extends PCEPSessionListener {

	public List<PCEPMessage> messages = new ArrayList<PCEPMessage>();

	public boolean up = false;

	private static final Logger logger = LoggerFactory.getLogger(SimpleSessionListener.class);

	public SimpleSessionListener() {
	}

	@Override
	public void onMessage(PCEPSession session, PCEPMessage message) {
		logger.debug("Received message: " + message.getClass() + " " + message);
		this.messages.add(message);
	}

	@Override
	public synchronized void onSessionUp(PCEPSession session, PCEPOpenObject local,
			PCEPOpenObject remote) {
		logger.debug("Session up.");
		this.up = true;
		this.notifyAll();
	}

	@Override
	public void onSessionDown(PCEPSession session, PCEPCloseObject reason, Exception e) {
		logger.debug("Session down.");
		this.up = false;
		//this.notifyAll();
	}

	@Override
	public void onSessionTerminated(PCEPSession session,
			TerminationReason cause) {
		logger.debug("Session terminated. Cause : " + cause.toString());
	}
}
