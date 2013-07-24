/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

/**
 * Enumeration of possibilities how a link can be protected.
 * @see <a href="http://tools.ietf.org/html/rfc4202#section-2.2">RFC 4202</a>
 */
public enum LinkProtectionType {
	/**
	 * If the link is of type Extra Traffic, it means that the link is
	 * protecting another link or links.  The LSPs on a link of this type
	 * will be lost if any of the links it is protecting fail.
	 */
	EXTRA_TRAFFIC,
	/**
	 * If the link is of type Unprotected, it means that there is no
	 * other link protecting this link.  The LSPs on a link of this type
	 * will be lost if the link fails.
	 */
	UNPROTECTED,
	/**
	 * If the link is of type Shared, it means that there are one or more
	 * disjoint links of type Extra Traffic that are protecting this
	 * link.  These Extra Traffic links are shared between one or more
	 * links of type Shared.
	 */
	SHARED,
	/**
	 * If the link is of type Dedicated 1:1, it means that there is one
	 * dedicated disjoint link of type Extra Traffic that is protecting
	 * this link.
	 */
	DEDICATED_ONE_TO_ONE,
	/**
	 * If the link is of type Dedicated 1+1, it means that a dedicated
	 * disjoint link is protecting this link.  However, the protecting
	 * link is not advertised in the link state database and is therefore
	 * not available for the routing of LSPs.
	 */
	DEDICATED_ONE_PLUS_ONE,
	/**
	 * If the link is of type Enhanced, it means that a protection scheme
	 * that is more reliable than Dedicated 1+1, e.g., 4 fiber
	 * BLSR/MS-SPRING, is being used to protect this link.
	 */
	ENHANCED
}

