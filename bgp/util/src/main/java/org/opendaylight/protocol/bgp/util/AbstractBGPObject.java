/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Implementation of BGPObject that wraps up BGPNode and BGPLink.
 */
public abstract class AbstractBGPObject implements BGPObject {
	private static final long serialVersionUID = 1L;
	private final BaseBGPObjectState state;

	protected AbstractBGPObject(final BaseBGPObjectState state) {
		this.state = state;
	}

	@Override
	public BaseBGPObjectState currentState() {
		return state;
	}

	@Override
	public synchronized final String toString(){
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("state", this.state);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractBGPObject other = (AbstractBGPObject) obj;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}
}
