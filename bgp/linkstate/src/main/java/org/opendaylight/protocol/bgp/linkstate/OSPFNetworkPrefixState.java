/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.NetworkAddress;
import com.google.common.base.Objects.ToStringHelper;

/**
 * OSPF-specific prefix prefix advertisement.
 * @param <T> Network Address type of the prefix being advertised
 */
public class OSPFNetworkPrefixState<T extends NetworkAddress<?>> extends NetworkPrefixState {
	private static final long serialVersionUID = 1L;
	private final T forwardingAddress;

	protected OSPFNetworkPrefixState(OSPFNetworkPrefixState<T> orig) {
		super(orig);
		this.forwardingAddress = orig.forwardingAddress;
	}

	public OSPFNetworkPrefixState(NetworkPrefixState orig, T forwardingAddress) {
		super(orig);
		this.forwardingAddress = forwardingAddress;
	}

	/**
	 * Returns the OSPF forwarding address attached to this advertisement.
	 *
	 * @return OSPF forwarding address
	 */
	public final T getForwardingAddress() {
		return forwardingAddress;
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("forwardingAddress", this.forwardingAddress);
		return super.addToStringAttributes(toStringHelper);
	}
}

