/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

/**
 * 
 * Common interface for MP_(UN)REACH Attribute.
 * 
 * @param <T> Link State NLRI, T represents an Identifier
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4760">MultiProtocol Extensions for BGP-4</a>
 */
public interface MPReach<T> {

	/**
	 * Determines if we have an MP_REACH or MP_UNREACH
	 * 
	 * @return true if the object is MP_REACH, false if its MP_UNREACH
	 */
	public boolean isReachable();

	/**
	 * 
	 * NLRI without Link-State information are just IP address prefixes, with Link-State inf. it can be also Network
	 * Links or Network Nodes.
	 * 
	 * @return set of objects present in NLRI
	 */
	public Set<T> getNlri();
}
