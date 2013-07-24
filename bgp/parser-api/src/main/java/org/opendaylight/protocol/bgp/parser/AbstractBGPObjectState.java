/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import org.opendaylight.protocol.bgp.concepts.BaseBGPObjectState;

import org.opendaylight.protocol.bgp.linkstate.NetworkObjectState;
import com.google.common.base.Objects.ToStringHelper;

public abstract class AbstractBGPObjectState<T extends NetworkObjectState> extends BaseBGPObjectState {
	private static final long serialVersionUID = 1L;
	private final T objectState;

	public AbstractBGPObjectState(final BaseBGPObjectState orig, final T objectState) {
		super(orig);
		this.objectState = objectState;
	}

	protected AbstractBGPObjectState(final AbstractBGPObjectState<T> orig) {
		super(orig);
		this.objectState = orig.objectState;
	}

	public final T getObjectState() {
		return this.objectState;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("objectState", this.objectState);
		return super.addToStringAttributes(toStringHelper);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.objectState == null) ? 0 : this.objectState.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractBGPObjectState<?> other = (AbstractBGPObjectState<?>) obj;
		if (this.objectState == null) {
			if (other.objectState != null)
				return false;
		} else if (!this.objectState.equals(other.objectState))
			return false;
		return true;
	}
}
