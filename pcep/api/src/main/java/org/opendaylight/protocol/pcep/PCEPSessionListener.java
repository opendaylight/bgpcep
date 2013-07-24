/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.TerminationReason;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 *	Listener that receives session informations from the session.
 */
public abstract class PCEPSessionListener implements SessionListener {

	/**
	 * Fired when a message is received.
	 * @param session session which received the message
	 * @param message PCEPMessage
	 */
	public abstract void onMessage(PCEPSession session, PCEPMessage message);

	/**
	 * Fired when the session is in state UP.
	 *
	 * @param session Session which went up
	 * @param local Local open proposal which the peer accepted
	 * @param remote Peer open proposal which we accepted
	 */
	public abstract void onSessionUp(PCEPSession session, PCEPOpenObject local, PCEPOpenObject remote);

	/**
	 * Fired when the session went down as a result of peer's decision
	 * to tear it down.
	 * Implementation should take care of closing underlying session.
	 *
	 * @param session Session which went down
	 * @param reason Reason for termination, may be null when the underlying
	 *               channel was closed without a specific reason.
	 * @param e exception that caused session down
	 */
	public abstract void onSessionDown(PCEPSession session, PCEPCloseObject reason, Exception e);

	/**
	 * Fired when the session is terminated locally. The session has already
	 * been closed and transitioned to IDLE state. Any outstanding queued
	 * messages were not sent. The user should not attempt to make any use
	 * of the session.
	 *
	 * @param session Session which went down
	 * @param cause the cause why the session went down
	 */
	public abstract void onSessionTerminated(PCEPSession session, TerminationReason cause);
}
