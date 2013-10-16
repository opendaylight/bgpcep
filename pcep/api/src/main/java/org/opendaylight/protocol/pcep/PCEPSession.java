/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;

import java.net.InetAddress;
import java.util.List;

/**
 * PCEP Session represents the finite state machine in PCEP, including timers and its purpose is to create a PCEP
 * connection between PCE/PCC. Session is automatically started, when TCP connection is created, but can be stopped
 * manually. If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown
 * requests.
 */
public interface PCEPSession extends ProtocolSession<PCEPMessage> {

	/**
	 * Sends message from user to PCE/PCC. If the user sends an Open Message, the session returns an error (open message
	 * is only allowed, when a PCEP handshake is in progress). Close message will close the session and free all the
	 * resources.
	 *
	 * @param message message to be sent
	 */
	public void sendMessage(PCEPMessage message);

	public void close(PCEPCloseObject.Reason reason);

    public List<PCEPTlv> getRemoteTlvs();

	public InetAddress getRemoteAddress();
}
