/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.parameter;

import java.util.Map;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;


/**
 * Graceful restart capability parameter as described in:
 * 
 * <a href="http://tools.ietf.org/html/rfc4724#section-3">Graceful Restart Capability</a>
 */
public final class GracefulCapability extends CapabilityParameter {

	/**
	 * Capability code for GR)
	 */
	private static final int CODE = 64;

	private final boolean restartFlag;

	private final int restartTimerValue;

	private final Map<BGPTableType, Boolean> tableTypes;

	/**
	 * Creates new Graceful restart capability.
	 * 
	 * @param restartFlag should be false
	 * @param restartTimerValue should be 0
	 * @param tableTypes supported AFI/SAFI along with Forwarding state flag (should be true)
	 */
	public GracefulCapability(final boolean restartFlag, final int restartTimerValue, final Map<BGPTableType, Boolean> tableTypes) {
		super(CODE);
		this.restartFlag = restartFlag;
		this.restartTimerValue = restartTimerValue;
		this.tableTypes = tableTypes;
	}

	/**
	 * Was router restarted?
	 * 
	 * @return the restartFlag
	 */
	public boolean isRestartFlag() {
		return this.restartFlag;
	}

	/**
	 * Currently should be always 0.
	 * 
	 * @return the restartTimerValue
	 */
	public int getRestartTimerValue() {
		return this.restartTimerValue;
	}

	/**
	 * Return supported AFI/SAFI along with Forwarding state flag.
	 * 
	 * @return the tableTypes
	 */
	public Map<BGPTableType, Boolean> getTableTypes() {
		return this.tableTypes;
	}
}
