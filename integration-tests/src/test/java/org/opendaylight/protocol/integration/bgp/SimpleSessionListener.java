/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.bgp;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Listener for the client.
 */
public class SimpleSessionListener implements BGPSessionListener {

	private final List<Notification> listMsg = Lists.newArrayList();

	public boolean up = false;

	private static final Logger logger = LoggerFactory.getLogger(SimpleSessionListener.class);

	public boolean down = false;

	public List<Notification> getListMsg() {
		return this.listMsg;
	}

	@Override
	public void onMessage(final BGPSession session, final Notification message) {
		this.listMsg.add(message);
		logger.debug("Message received:" + message);
	}

	@Override
	public void onSessionUp(final BGPSession session) {
		logger.debug("Session Up");
		this.up = true;
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		logger.debug("Session Down", e);
		this.down = true;
	}

	@Override
	public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
		logger.debug("Session terminated. Cause : " + cause.toString());
	}
}
