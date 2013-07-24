/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;

public final class BGPPrefixState extends AbstractBGPObjectState<NetworkPrefixState> {
	private static final long serialVersionUID = 1L;

	public BGPPrefixState(BaseBGPObjectState base, NetworkPrefixState prefixState) {
		super(base, prefixState);
	}

	protected BGPPrefixState(BGPPrefixState orig) {
		super(orig, orig.getObjectState());
	}

	@Override
	protected BGPPrefixState newInstance() {
		return new BGPPrefixState(this);
	}
}
