/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.SortedSet;

import org.opendaylight.protocol.bgp.linkstate.ExtendedRouteTag;
import com.google.common.base.Objects.ToStringHelper;

/**
 * ISIS-specific prefix prefix advertisement state.
 */
public class ISISNetworkPrefixState extends NetworkPrefixState {
	private static final long serialVersionUID = 1L;
	private final SortedSet<ExtendedRouteTag> extendedRouteTags;
	private final boolean upDownBit;

	public ISISNetworkPrefixState(NetworkPrefixState orig, SortedSet<ExtendedRouteTag> extendedRouteTags, boolean upDownBit) {
		super(orig);
		this.extendedRouteTags = extendedRouteTags;
		this.upDownBit = upDownBit;
	}

	protected ISISNetworkPrefixState(ISISNetworkPrefixState orig) {
		super(orig);
		this.extendedRouteTags = orig.extendedRouteTags;
		this.upDownBit = orig.upDownBit;
	}

	/**
	 * Returns the IS-IS extended route tags associated with this
	 * advertisement.
	 *
	 * @return IS-IS extended route tags, may be empty
	 */
	public final SortedSet<ExtendedRouteTag> getExtendedRouteTags() {
		return extendedRouteTags;
	}

	/**
	 * Returns the IS-IS Up/Down bit associated with this advertisement.
	 *
	 * @return Status of the IS-IS Up/Down bit
	 */
	public final boolean getUpDownBit() {
		return upDownBit;
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("extendedRouteTags", this.extendedRouteTags);
		toStringHelper.add("upDownBit", this.upDownBit);
		return super.addToStringAttributes(toStringHelper);
	}
}

