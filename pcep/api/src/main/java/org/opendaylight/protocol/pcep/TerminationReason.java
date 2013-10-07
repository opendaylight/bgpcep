/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.util.Map;

import com.google.common.collect.Maps;

public enum TerminationReason {
	Unknown((short) 1), ExpDeadtimer((short) 2), MalformedMsg((short) 3), TooManyUnknownReqRep((short) 4), TooManyUnknownMsg((short) 5);

	short value;
	static Map<Short, TerminationReason> valueMap;

	static {
		valueMap = Maps.newHashMap();
		for (final TerminationReason enumItem : TerminationReason.values()) {
			valueMap.put(enumItem.value, enumItem);
		}
	}

	private TerminationReason(final short value) {
		this.value = value;
	}

	/**
	 * @return integer value
	 */
	public short getShortValue() {
		return this.value;
	}

	/**
	 * @param valueArg
	 * @return corresponding TerminationReason item
	 */
	public static TerminationReason forValue(final short valueArg) {
		return valueMap.get(valueArg);
	}
}
