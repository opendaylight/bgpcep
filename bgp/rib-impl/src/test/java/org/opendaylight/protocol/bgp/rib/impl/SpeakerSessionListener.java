/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.List;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Listener for the BGP Speaker.
 */
public class SpeakerSessionListener implements BGPSessionListener {

	public List<BGPMessage> messages = Lists.newArrayList();

	public boolean up = false;

	public Set<BGPTableType> types;

	private static final Logger logger = LoggerFactory.getLogger(SpeakerSessionListener.class);

	public SpeakerSessionListener() {
	}

	@Override
	public void onMessage(final BGPSession session, final BGPMessage message) {
		logger.debug("Received message: " + message.getClass() + " " + message);
		this.messages.add(message);
	}

	@Override
	public synchronized void onSessionUp(final BGPSession session) {
		logger.debug("Session up.");
		this.up = true;
		this.types = session.getAdvertisedTableTypes();
		this.notifyAll();
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		logger.debug("Session down.");
		this.up = false;
	}

	@Override
	public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
		logger.debug("Session terminated. Cause : " + cause.toString());
		this.up = false;
	}
}
