/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

/**
 * Enumeration of all supported signalling protocols which support tunnel
 * establishment in a MPLS network.
 */
public enum MPLSProtocol {
	/**
	 * Label Distribution Protocol. Specified by <a href="http://tools.ietf.org/html/rfc3036">RFC 3036</a>.
	 */
	LDP,
	/**
	 * Resource Reservation Protocol - Traffic Engineering. Specified by
	 * <a href="http://tools.ietf.org/html/rfc3209">RFC 3209</a>.
	 */
	RSVPTE
}

