/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;

public final class BGPRouteState<T extends NetworkAddress<?>> extends AbstractBGPObjectState<NetworkRouteState<T>> {
	private static final long serialVersionUID = 1L;

	public BGPRouteState(BaseBGPObjectState orig, NetworkRouteState<T> routeState) {
		super(orig, routeState);
	}

	protected BGPRouteState(BGPRouteState<T> orig) {
		super(orig, orig.getObjectState());
	}

	@Override
	protected BGPRouteState<T> newInstance() {
		return new BGPRouteState<T>(this);
	}
}
