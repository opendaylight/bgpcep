/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import org.opendaylight.protocol.bgp.linkstate.NetworkNodeState;

public final class BGPNodeState extends AbstractBGPObjectState<NetworkNodeState> {
	private static final long serialVersionUID = 1L;

	public BGPNodeState(BaseBGPObjectState orig, NetworkNodeState nodeState) {
		super(orig, nodeState);
	}

	protected BGPNodeState(BGPNodeState orig) {
		super(orig, orig.getObjectState());
	}

	@Override
	protected BGPNodeState newInstance() {
		return new BGPNodeState(this);
	}
}
