/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.framework.DispatcherImpl;

public class SpeakerSessionListener extends BGPSessionListener {
	private static final Logger logger = LoggerFactory.getLogger(SpeakerSessionListener.class);

	DispatcherImpl d;

	SpeakerSessionListener(final DispatcherImpl d) {
		this.d = d;
	}

	@Override
	public void onSessionUp(final Set<BGPTableType> remote) {
		logger.info("Server: Session is up.");
	}

	@Override
	public void onSessionTerminated(final BGPError cause) {
		logger.info("Server: Session terminated: {}", cause);
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		logger.info("Server: Session down.");
		session.close();
		this.d.stop();
	}

	@Override
	public void onMessage(final BGPMessage message) {
		logger.info("Server: Message received: {}", message);
		this.d.stop();
	}
}
