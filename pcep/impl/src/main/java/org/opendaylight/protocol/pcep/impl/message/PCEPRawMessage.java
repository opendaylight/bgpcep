/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageFactory.PCEPMessageType;

/**
 * Class representing raw message.
 */
public class PCEPRawMessage extends PCEPMessage {

	private static final long serialVersionUID = 1075879993862417873L;

	private final PCEPMessageType msgType;

	public PCEPRawMessage(List<PCEPObject> objects, PCEPMessageType msgType) {
		super(objects);
		this.msgType = msgType;
	}

	public PCEPMessageType getMsgType() {
		return this.msgType;
	}

}
