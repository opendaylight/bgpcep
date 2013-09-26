/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.linkstate.NetworkRouteState;

public final class BGPRouteState extends AbstractBGPObjectState<NetworkRouteState> {
	private static final long serialVersionUID = 1L;

	public BGPRouteState(final BaseBGPObjectState orig, final NetworkRouteState routeState) {
		super(orig, routeState);
	}

	protected BGPRouteState(final BGPRouteState orig) {
		super(orig, orig.getObjectState());
	}

	@Override
	protected BGPRouteState newInstance() {
		return new BGPRouteState(this);
	}
}
