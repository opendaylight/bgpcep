/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.util.Timer;

public final class SimpleSessionFactory implements ProtocolSessionFactory {
	private final int maximumMessageSize;

	public SimpleSessionFactory(int maximumMessageSize) {
		this.maximumMessageSize = maximumMessageSize;
	}

	@Override
	public ProtocolSession getProtocolSession(SessionParent parent, Timer timer, ProtocolConnection connection, int sessionId) {
		return new SimpleSession(connection, parent, maximumMessageSize);
	}
}
