/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;


/**
 * Update event indicating that a RIB has reached initial synchronization. An instance of this interface is generated
 * when the BGP UPDATE parser encounters such a marker message.
 */
public interface BGPUpdateSynchronized extends BGPUpdateEvent {
	/**
	 * Identify which RIB has reached synchronization.
	 * 
	 * @return BGP table type
	 */
	public BGPTableType getTableType();
}
