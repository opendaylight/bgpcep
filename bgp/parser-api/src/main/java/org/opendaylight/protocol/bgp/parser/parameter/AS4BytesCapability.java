/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.parameter;

import org.opendaylight.protocol.concepts.ASNumber;

/**
 * AS 4B Numbers capability.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc6793">BGP Support for 4-Octet AS Number Space</a>
 */
public final class AS4BytesCapability extends CapabilityParameter {

	/**
	 * Capability code for 4B AS.
	 */
	public static final int CODE = 65;

	private final ASNumber as;

	public AS4BytesCapability(final ASNumber as) {
		super(CODE);
		this.as = as;
	}

	/**
	 * Returns 4B AS Number.
	 * 
	 * @return 4B AS Number
	 */
	public ASNumber getASNumber() {
		return this.as;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AS4BytesCapability [as=");
		builder.append(this.as);
		builder.append("]");
		return builder.toString();
	}
}
