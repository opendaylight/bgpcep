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
 * @see <a href="http://tools.ietf.org/html/rfc4360#section-4">Route Target Community</a>
 */
public class RouteTargetCommunity extends ASSpecificExtendedCommunity {

	private static final long serialVersionUID = 855252238770644603L;

	/**
	 * 
	 * @param globalAdmin Globally-administered identifier, i.e. an {@link AsNumber}
	 * @param localAdmin Locally-administered identifier
	 */
	public RouteTargetCommunity(final AsNumber globalAdmin, final byte[] localAdmin) {
		super(false, 2, globalAdmin, localAdmin);
	}
}
