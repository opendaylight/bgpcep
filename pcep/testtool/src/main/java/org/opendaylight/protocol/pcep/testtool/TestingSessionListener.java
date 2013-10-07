/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class TestingSessionListener implements PCEPSessionListener {

	public List<Message> messages = Lists.newArrayList();

	public boolean up = false;

	private static final Logger logger = LoggerFactory.getLogger(TestingSessionListener.class);

	public TestingSessionListener() {
	}

	@Override
	public void onMessage(final PCEPSession session, final Message message) {
		logger.debug("Received message: {}", message);
		this.messages.add(message);
	}

	@Override
	public void onSessionUp(final PCEPSession session) {
		logger.debug("Session up.");
		this.up = true;
		// this.notifyAll();
	}

	@Override
	public void onSessionDown(final PCEPSession session, final Exception e) {
		logger.debug("Session down. Cause : {} or {}", e);
		this.up = false;
		// this.notifyAll();
	}

	@Override
	public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
		logger.debug("Session terminated. Cause : {}", cause);
	}
}
