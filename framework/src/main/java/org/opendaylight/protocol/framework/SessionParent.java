/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.Closeable;

/**
 * Interface that groups together the classes that can create a session (Dispatcher and Server). When a session
 * is closing, it has to notify its parent about closing. Each parent keeps a Map of its sessions. When some session closes,
 * it fires onSessionClosed event with its own instance as parameter and the parent of this session will remove it from his map.
 */
public interface SessionParent extends Closeable {

	/**
	 * This listener method is called when a session that was created by a class implementing this interface,
	 * is closing. Implementation should remove corresponding session from its list of sessions.
	 * @param session a session that is closing
	 */
	public void onSessionClosed(final ProtocolSession session);

	/**
	 * This listener method is called when a session has produced some output and the parent needs to react to
	 * it.
	 * @param session a session that has produced output
	 */
	public void checkOutputBuffer(final ProtocolSession session);
}
