/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Timer;

import org.opendaylight.protocol.bgp.parser.BGPMessageParser;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;

import org.opendaylight.protocol.framework.ProtocolConnection;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ProtocolSessionFactory;
import org.opendaylight.protocol.framework.SessionParent;

/**
 *
 */
public final class BGPSessionFactory implements ProtocolSessionFactory {

	private final BGPMessageParser parser;

	public BGPSessionFactory(final BGPMessageParser parser) {
		this.parser = parser;
	}

	@Override
	public ProtocolSession getProtocolSession(final SessionParent parent, final Timer timer, final ProtocolConnection connection,
			final int sessionId) {
		return new BGPSessionImpl(parent, timer, (BGPConnection) connection, sessionId, this.parser);
	}
}
