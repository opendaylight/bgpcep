/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;

import org.opendaylight.protocol.framework.SessionListener;

/**
 * Listener that receives session informations from the session.
 */
public abstract class BGPSessionListener implements SessionListener {

	/**
	 * Fired when an Update or a non-fatal Notification message is received.
	 * 
	 * @param message BGPMessage
	 */
	public abstract void onMessage(BGPMessage message);

	/**
	 * Fired when the session was established successfully.
	 * 
	 * @param remoteParams Peer address families which we accepted
	 */
	public abstract void onSessionUp(Set<BGPTableType> remoteParams);

	/**
	 * Fired when the session went down because of an IO error. Implementation should take care of closing underlying
	 * session.
	 * 
	 * @param session that went down
	 * @param e Exception that was thrown as the cause of session being down
	 */
	public abstract void onSessionDown(BGPSession session, Exception e);

	/**
	 * Fired when the session is terminated locally. The session has already been closed and transitioned to IDLE state.
	 * Any outstanding queued messages were not sent. The user should not attempt to make any use of the session.
	 * 
	 * @param cause the cause why the session went down
	 */
	public abstract void onSessionTerminated(BGPError cause);
}
