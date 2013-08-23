/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.Channel;

import java.util.Timer;

/**
 * Factory for generating Protocol Sessions. This class should be extended to return protocol specific session.
 */
public interface ProtocolSessionFactory<T extends ProtocolSession> {

	/**
	 * Creates and returns protocol specific session.
	 * 
	 * @param parent SessionParent
	 * @param timer Timer
	 * @param connection connection attributes
	 * @param sessionId session identifier
	 * @return new session
	 */
	public T getProtocolSession(SessionParent dispatcher, Timer timer, ProtocolConnection connection, int sessionId,
			Channel channel);
}
