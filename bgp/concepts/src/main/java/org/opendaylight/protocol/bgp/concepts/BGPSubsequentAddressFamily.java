/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

/**
 * Enumeration of all supported subsequent address family identifiers.
 */
public enum BGPSubsequentAddressFamily {
	/**
	 * RFC4760: Network Layer Reachability Information used
	 * for unicast forwarding.
	 */
	Unicast,
	/**
	 * RFC4364: MPLS-labeled VPN address
	 */
	MPLSLabeledVPN,
	/**
	 * draft-ietf-idr-ls-distribution-03 : Linkstate SAFI
	 */
	Linkstate
}
