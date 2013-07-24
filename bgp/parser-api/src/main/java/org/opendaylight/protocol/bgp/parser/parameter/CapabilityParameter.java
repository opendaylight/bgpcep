/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.parameter;

import org.opendaylight.protocol.bgp.parser.BGPParameter;

/**
 * Capability parameter superclass.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3392#section-4">Capabilities Optional Parameter</a>
 */
public abstract class CapabilityParameter implements BGPParameter {

	private final int TYPE = 2;

	private final int code;

	CapabilityParameter(final int code) {
		this.code = code;
	}

	/**
	 * Returns capability code specific capability parameter.
	 * 
	 * @return capability code
	 */
	public int getCode() {
		return this.code;
	}

	@Override
	public int getType() {
		return this.TYPE;
	}
}
