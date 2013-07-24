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
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 *
 */
public class MockPCE extends PCEPSessionListener {

	private final List<PCEPMessage> listMsg = new ArrayList<PCEPMessage>();

	private PCEPSessionImpl session = null;

	public boolean up = false;

	private static final Logger logger = LoggerFactory.getLogger(MockPCE.class);

	public boolean down = false;

	public MockPCE() {
	}

	public void sendMessage(PCEPMessage msg) {
		this.session.handleMessage(msg);
	}

	public void sendErrorMessage(PCEPErrors value,
			PCEPOpenObject open) {
		final PCEPErrorObject error = new PCEPErrorObject(value);
		final List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();
		errors.add(error);
		this.sendMessage(new PCEPErrorMessage(open, errors, null));
	}

	public List<PCEPMessage> getListMsg() {
		return this.listMsg;
	}

	public void addSession(PCEPSessionImpl l) {
		this.session = l;
	}

	@Override
	public void onMessage(PCEPSession session, PCEPMessage message) {
		this.listMsg.add(message);
		logger.debug("Message received:" + message);
	}

	@Override
	public void onSessionUp(PCEPSession session, PCEPOpenObject local,
			PCEPOpenObject remote) {
		logger.debug("Session Up");
		this.up = true;
		this.notifyAll();
	}

	@Override
	public void onSessionDown(PCEPSession session, PCEPCloseObject reason, Exception e) {
		logger.debug("Session Down");
		this.down = true;
		//this.notifyAll();
	}

	@Override
	public void onSessionTerminated(PCEPSession session,
			TerminationReason cause) {
		logger.debug("Session terminated. Cause : " + cause.toString());
	}
}
