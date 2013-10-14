/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * Class representing raw message.
 */
public class RawMessage implements Message {
	private final PCEPMessageType msgType;
	private final List<PCEPObject> objects;

	public RawMessage(final List<PCEPObject> objects, final PCEPMessageType msgType) {
		this.msgType = msgType;
		if (objects.contains(null)) {
			throw new IllegalArgumentException("Object list contains null element at offset " + objects.indexOf(null));
		}
		this.objects = objects;
	}

	public PCEPMessageType getMsgType() {
		return this.msgType;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public Class<Message> getImplementedInterface() {
		return Message.class;
	}
}
