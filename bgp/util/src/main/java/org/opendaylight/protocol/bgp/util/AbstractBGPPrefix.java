/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;
import org.opendaylight.protocol.bgp.parser.BGPPrefix;
import org.opendaylight.protocol.bgp.parser.BGPPrefixState;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;
import org.opendaylight.protocol.bgp.linkstate.NetworkPrefixState;
import com.google.common.base.Preconditions;

/**
 * Super class for BGPIpvXPrefixImpl.
 * 
 * @param <T> extends NetworkAddress<T>
 */
public abstract class AbstractBGPPrefix<T extends NetworkAddress<T>> extends AbstractBGPObject implements BGPPrefix<T> {
	private static final long serialVersionUID = 1L;
	private final PrefixIdentifier<T> descriptor;

	protected AbstractBGPPrefix(final BaseBGPObjectState base, final PrefixIdentifier<T> descriptor, final NetworkPrefixState prefixState) {
		super(new BGPPrefixState(base, prefixState));
		Preconditions.checkNotNull(descriptor);
		this.descriptor = descriptor;
	}

	@Override
	public final PrefixIdentifier<T> getPrefixIdentifier() {
		return this.descriptor;
	}

	@Override
	final public BGPPrefixState currentState() {
		return (BGPPrefixState) super.currentState();
	}
}
