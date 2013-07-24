/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BGPObject;

import org.opendaylight.protocol.concepts.NamedObject;
import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.concepts.Prefix;

/**
 * A route is a unit of information that pairs a set of destinations with the attributes of a path to those
 * destinations. The set of destinations are systems whose IP addresses are contained in one IP address prefix carried
 * in the Network Layer Reachability Information (NLRI) field of an UPDATE message. The path is the information reported
 * in the path attributes field of the same UPDATE message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-1.1">Definition of Commonly Used Terms</a>
 * @param <T> subtype of Network Address
 */
public interface BGPRoute<T extends NetworkAddress<T>> extends BGPObject, NamedObject<Prefix<T>> {
	
	@Override
	public BGPRouteState<T> currentState();
}
