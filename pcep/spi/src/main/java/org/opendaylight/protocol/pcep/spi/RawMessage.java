/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Class representing raw message.
 */
public class RawMessage extends PCEPMessage {
	private final PCEPMessageType msgType;

	public RawMessage(final List<PCEPObject> objects, final PCEPMessageType msgType) {
		super(objects);
		this.msgType = msgType;
	}

	public PCEPMessageType getMsgType() {
		return this.msgType;
	}

}
