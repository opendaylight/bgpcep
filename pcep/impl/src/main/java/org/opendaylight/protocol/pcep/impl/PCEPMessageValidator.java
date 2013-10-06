/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.HashMap;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.message.PCCreateMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPReportMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageValidator;
import org.opendaylight.protocol.pcep.impl.message.PCEPUpdateRequestMessageValidator;
import org.opendaylight.protocol.pcep.spi.PCEPMessageType;

/**
 * Base class for message validators
 */
public abstract class PCEPMessageValidator {

	private static class MapOfValidators extends HashMap<PCEPMessageType, PCEPMessageValidator> {

		private static final long serialVersionUID = -5715193806554448822L;

		private final static MapOfValidators instance = new MapOfValidators();

		private MapOfValidators() {
			this.fillInMap();
		}

		private void fillInMap() {
			this.put(PCEPMessageType.OPEN, new PCEPOpenMessageValidator());
			this.put(PCEPMessageType.KEEPALIVE, new PCEPKeepAliveMessageValidator());
			this.put(PCEPMessageType.NOTIFICATION, new PCEPNotificationMessageValidator());
			this.put(PCEPMessageType.ERROR, new PCEPErrorMessageValidator());
			this.put(PCEPMessageType.RESPONSE, new PCEPReplyMessageValidator());
			this.put(PCEPMessageType.REQUEST, new PCEPRequestMessageValidator());
			this.put(PCEPMessageType.UPDATE_REQUEST, new PCEPUpdateRequestMessageValidator());
			this.put(PCEPMessageType.STATUS_REPORT, new PCEPReportMessageValidator());
			this.put(PCEPMessageType.CLOSE, new PCEPCloseMessageValidator());
			this.put(PCEPMessageType.PCCREATE, new PCCreateMessageValidator());
		}

		public static MapOfValidators getInstance() {
			return instance;
		}
	}

	public abstract List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException;

	public static PCEPMessageValidator getValidator(final PCEPMessageType msgType) {
		return MapOfValidators.getInstance().get(msgType);
	}
}
