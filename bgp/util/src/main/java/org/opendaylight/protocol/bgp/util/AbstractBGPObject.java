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
		return this.state;
	}

	@Override
	public synchronized String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("state", this.state);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.state == null) ? 0 : this.state.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractBGPObject other = (AbstractBGPObject) obj;
		if (this.state == null) {
			if (other.state != null)
				return false;
		} else if (!this.state.equals(other.state))
			return false;
		return true;
	}
}
