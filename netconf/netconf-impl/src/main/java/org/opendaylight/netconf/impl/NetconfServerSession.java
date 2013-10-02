/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import java.io.IOException;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.api.NetconfTerminationReason;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class NetconfServerSession extends NetconfSession {

	private final SessionListener sessionListener;
	private final Channel channel;

	private static final Logger logger = LoggerFactory.getLogger(NetconfServerSession.class);
	private final long sessionId;
	private boolean up = false;

	public NetconfServerSession(SessionListener sessionListener, Channel channel, long sessionId) {
		this.sessionListener = sessionListener;
		this.channel = channel;
		this.sessionId = sessionId;
		logger.debug("Session {} created", toString());
	}

	@Override
	public void close() {
		sessionListener.onSessionTerminated(this, new NetconfTerminationReason("Session closed"));
	}

	@Override
	protected void handleMessage(NetconfMessage netconfMessage) {
		logger.debug("Session {} received message {}", toString(), Xml.toString(netconfMessage.getDocument()));
		sessionListener.onMessage(this, netconfMessage);
	}

	public void sendMessage(NetconfMessage netconfMessage) {
		channel.writeAndFlush(netconfMessage);
	}

	@Override
	protected void endOfInput() {
		logger.debug("Session {} end of input detected while session was in state {}", toString(), isUp() ? "up" : "initialized");
		if (isUp()) {
			this.sessionListener.onSessionDown(this, new IOException("End of input detected. Close the session."));
		}
	}

	@Override
	protected void sessionUp() {
		logger.debug("Session {} up", toString());
		sessionListener.onSessionUp(this);
		this.up = true;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("ServerNetconfSession{");
		sb.append("sessionId=").append(sessionId);
		sb.append('}');
		return sb.toString();
	}

	public boolean isUp() {
		return up;
	}

	public long getSessionId() {
		return sessionId;
	}
}
