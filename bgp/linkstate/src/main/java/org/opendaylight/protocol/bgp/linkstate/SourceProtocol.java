/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

/**
 * Enumeration of all protocols which may source link-state information.
 */
public enum SourceProtocol {
	/**
	 * IS-IS Level 1 adjacency
	 */
	ISISLevel1,
	/**
	 * IS-IS Level 2 adjacency
	 */
	ISISLevel2,
	/**
	 * OSPF adjacency
	 */
	OSPF,
	/**
	 * Local run-time state. Learned by the advertising entity by
	 * seeing its directly-attached neighbors.
	 */
	Direct,
	/**
	 * Local configuration. Defined statically by an operator at the
	 * advertising entity.
	 */
	Static,
	/**
	 * Unknown source. Use this value if the others do not fit how we
	 * came by the information.
	 */
	Unknown
}

