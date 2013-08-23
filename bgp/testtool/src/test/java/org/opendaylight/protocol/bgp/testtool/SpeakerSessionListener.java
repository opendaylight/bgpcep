/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.BGPTerminationReason;
import org.opendaylight.protocol.framework.DispatcherImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeakerSessionListener implements BGPSessionListener {
	private static final Logger logger = LoggerFactory.getLogger(SpeakerSessionListener.class);

	DispatcherImpl d;

	SpeakerSessionListener(final DispatcherImpl d) {
		this.d = d;
	}

	@Override
	public void onSessionUp(final BGPSession session) {
		logger.info("Server: Session is up.");
	}

	@Override
	public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
		logger.info("Server: Session terminated: {}", cause);
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		logger.info("Server: Session down.");
		session.close();
		// this.d.stop();
	}

	@Override
	public void onMessage(final BGPSession session, final BGPMessage message) {
		logger.info("Server: Message received: {}", message);
		// this.d.stop();
	}
}
