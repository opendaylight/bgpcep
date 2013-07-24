/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.NetworkAddress;

/**
 * 
 * BGP Path Attribute AGGREGATOR.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-5.1.7">BGP Aggregator</a>
 * 
 */
public interface BGPAggregator {

	/**
	 * The BGP Speaker returns its own AS Number.
	 * 
	 * @return own AS Number
	 */
	public ASNumber getASNumber();

	/**
	 * The BGP Speaker returns its own IP Address.
	 * 
	 * @return own IP Address
	 */
	public NetworkAddress<?> getNetworkAddress();
}
