/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.util.ArrayList;
import java.util.List;

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

	public TestingSessionListener() {
	}

	@Override
	public void onMessage(final PCEPSession session, final PCEPMessage message) {
		logger.debug("Received message: {}", message);
		this.messages.add(message);
	}

	@Override
	public void onSessionUp(final PCEPSession session, final PCEPOpenObject local, final PCEPOpenObject remote) {
		logger.debug("Session up.");
		final List<ExplicitRouteSubobject> subs = new ArrayList<ExplicitRouteSubobject>();
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 10, 1, 1, 2 }), 32), false));
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 2, 2, 2, 2 }), 32), false));
		session.sendMessage(new PCEPXRAddTunnelMessage(new PCEPLspObject(23, false, false, false, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(new byte[] {
				1, 1, 1, 1 }), new IPv4Address(new byte[] { 2, 2, 2, 2 })), new PCEPExplicitRouteObject(subs, false)));
	}

	@Override
	public void onSessionDown(final PCEPSession session, final PCEPCloseObject reason, final Exception e) {
		logger.debug("Session down because: {}", reason);
	}

	@Override
	public void onSessionTerminated(final PCEPSession session, final TerminationReason cause) {
		logger.debug("Session terminated. Cause : {}", cause.toString());
	}
}
