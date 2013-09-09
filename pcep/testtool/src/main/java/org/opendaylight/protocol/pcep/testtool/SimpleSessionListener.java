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

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.message.PCCreateMessage;
import org.opendaylight.protocol.pcep.object.CompositeInstantiationObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Simple Session Listener that is notified about messages and changes in the session.
 */
public class SimpleSessionListener implements PCEPSessionListener {

	public List<PCEPMessage> messages = new ArrayList<PCEPMessage>();

	private static final Logger logger = LoggerFactory.getLogger(SimpleSessionListener.class);

	public SimpleSessionListener() {
	}

	@Override
	public void onMessage(final PCEPSession session, final PCEPMessage message) {
		logger.debug("Received message: {}", message);
		this.messages.add(message);
	}

	@Override
	public void onSessionUp(final PCEPSession session) {
		logger.debug("Session up.");
		final List<ExplicitRouteSubobject> subs = new ArrayList<ExplicitRouteSubobject>();
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 10, 1, 1, 2 }), 32), false));
		subs.add(new EROIPPrefixSubobject<Prefix<?>>(new IPv4Prefix(new IPv4Address(new byte[] { 2, 2, 2, 2 }), 32), false));
		final CompositeInstantiationObject cpo = new CompositeInstantiationObject(new PCEPEndPointsObject<IPv4Address>(IPv4.FAMILY.addressForBytes(new byte[] {
				1, 1, 1, 1 }), IPv4.FAMILY.addressForBytes(new byte[] { 2, 2, 2, 2 })), null, new PCEPExplicitRouteObject(subs, false), null, null);

		session.sendMessage(new PCCreateMessage(Lists.newArrayList(cpo)));
	}

	@Override
	public void onSessionDown(final PCEPSession session, final Exception e) {
		logger.debug("Session down with cause : {} or exception: {}", e);
		session.close();
	}

	@Override
	public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
		logger.debug("Session terminated. Cause : {}", cause.toString());
	}
}
