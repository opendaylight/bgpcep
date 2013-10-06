/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type identifier for {@link org.opendaylight.protocol.pcep.PCEPMessage PCEPMessage}
 */
@Deprecated()
public enum PCEPMessageType {
	OPEN(1), NOTIFICATION(5), KEEPALIVE(2), RESPONSE(4), REQUEST(3), ERROR(6), CLOSE(7), UPDATE_REQUEST(11), STATUS_REPORT(10),
	// TODO: replace with actual values by IANA
	PCCREATE(12);

	private static final Logger logger = LoggerFactory.getLogger(PCEPMessageType.class);
	private final int identifier;

	PCEPMessageType(final int identifier) {
		this.identifier = identifier;
	}

	public int getIdentifier() {
		return this.identifier;
	}

	public static PCEPMessageType getFromInt(final int type) {

		for (final PCEPMessageType type_e : PCEPMessageType.values()) {
			if (type_e.getIdentifier() == type) {
				return type_e;
			}
		}

		logger.trace("Unknown PCEPMessage Class identifier. Passed: {}; Known: {}", type, values());
		return null;
	}
}