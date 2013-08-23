/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.Channel;

import java.util.Timer;

import org.opendaylight.protocol.framework.ProtocolConnection;
import org.opendaylight.protocol.framework.ProtocolSessionFactory;
import org.opendaylight.protocol.framework.SessionParent;
import org.opendaylight.protocol.pcep.PCEPConnection;

public class PCEPSessionFactoryImpl implements ProtocolSessionFactory<PCEPSessionImpl> {

	private final int maxUnknownMessages;

	public PCEPSessionFactoryImpl(final int maxUnknownMessages) {
		this.maxUnknownMessages = maxUnknownMessages;
	}

	@Override
	public PCEPSessionImpl getProtocolSession(final SessionParent parent, final Timer timer, final ProtocolConnection connection,
			final int sessionId, final Channel channel) {
		return new PCEPSessionImpl(parent, timer, (PCEPConnection) connection, new PCEPMessageFactory(), this.maxUnknownMessages, sessionId, channel);
	}
}
