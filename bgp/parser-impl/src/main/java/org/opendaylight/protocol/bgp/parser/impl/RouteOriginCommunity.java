/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.concepts.ASSpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * @see <a href="http://tools.ietf.org/html/rfc4360#section-5">Route Origin Community</a>
 */
public class RouteOriginCommunity extends ASSpecificExtendedCommunity {

	private static final long serialVersionUID = 540725495203637583L;

	/**
	 * Construct a RouteOriginCommunity based on global and local administrator values.
	 * 
	 * @param globalAdmin Global administrator (AS number)
	 * @param localAdmin Local administrator (AS-specific, opaque value)
	 */
	public RouteOriginCommunity(final AsNumber globalAdmin, final byte[] localAdmin) {
		super(false, 3, globalAdmin, localAdmin);
	}
}
