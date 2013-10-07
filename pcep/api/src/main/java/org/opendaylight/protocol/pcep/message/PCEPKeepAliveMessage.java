/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * Structure of Keepalive Message
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.3">KeepAlive Message</a>
 */
public final class PCEPKeepAliveMessage implements Message {

	/**
	 * Default constructor PCEPKeepAliveMessage.
	 */
	public PCEPKeepAliveMessage() {
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPKeepAliveMessage []");
		return builder.toString();
	}
}
