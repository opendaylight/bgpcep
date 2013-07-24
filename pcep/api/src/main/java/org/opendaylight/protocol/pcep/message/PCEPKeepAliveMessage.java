/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.Collections;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Structure of Keepalive Message
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.3">KeepAlive
 *      Message</a>
 */
public final class PCEPKeepAliveMessage extends PCEPMessage {

	private static final long serialVersionUID = 8133032616718362219L;

	/**
	 * Default constructor PCEPKeepAliveMessage.
	 */
	public PCEPKeepAliveMessage() {
		super(Collections.<PCEPObject> emptyList());
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPKeepAliveMessage []");
		return builder.toString();
	}
}
