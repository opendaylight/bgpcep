/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.Prefix;

/**
 * Implementation of {@link AbstractBGPPrefix}.
 */
public final class BGPIPv6RouteImpl extends AbstractBGPRoute<IPv6Address> {
	private static final long serialVersionUID = 1L;

	public BGPIPv6RouteImpl(final Prefix<IPv6Address> prefix, final BaseBGPObjectState base, final NetworkRouteState prefixState) {
		super(prefix, base, prefixState);
	}
}
