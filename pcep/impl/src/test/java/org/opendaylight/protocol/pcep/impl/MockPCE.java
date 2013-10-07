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

import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockPCE implements PCEPSessionListener {

	private final List<Message> listMsg = new ArrayList<Message>();

	private PCEPSessionImpl session = null;

	public boolean up = false;

	private static final Logger logger = LoggerFactory.getLogger(MockPCE.class);

	public boolean down = false;

	public MockPCE() {
	}

	public void sendMessage(final Message msg) {
		this.session.handleMessage(msg);
	}

	public void sendErrorMessage(final PCEPErrors value, final PCEPOpenObject open) {
		final PCEPErrorObject error = new PCEPErrorObject(value);
		final List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();
		errors.add(error);
		this.sendMessage(new PCEPErrorMessage(open, errors, null));
	}

	public List<Message> getListMsg() {
		return this.listMsg;
	}

	public void addSession(final PCEPSessionImpl l) {
		this.session = l;
	}

	@Override
	public void onMessage(final PCEPSession session, final Message message) {
		this.listMsg.add(message);
		logger.debug("Message received: {}", message);
	}

	@Override
	public void onSessionUp(final PCEPSession session) {
		logger.debug("Session Up");
		this.up = true;
		this.notifyAll();
	}

	@Override
	public void onSessionDown(final PCEPSession session, final Exception e) {
		logger.debug("Session Down.", e);
		this.down = true;
		// this.notifyAll();
	}

	@Override
	public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
		logger.debug("Session terminated. Cause : {}", cause);
	}
}
