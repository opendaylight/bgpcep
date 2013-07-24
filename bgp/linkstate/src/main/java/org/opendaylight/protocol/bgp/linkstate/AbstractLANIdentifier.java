/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

public abstract class AbstractLANIdentifier<T extends RouterIdentifier> implements LANIdentifier<T> {
	private static final long serialVersionUID = 1L;
	private final T designatedRouter;

	protected AbstractLANIdentifier(final T designatedRouter) {
		this.designatedRouter = Preconditions.checkNotNull(designatedRouter);
	}

	@Override
	final public T getDesignatedRouter() {
		return this.designatedRouter;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("designatedRouter", this.designatedRouter);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((designatedRouter == null) ? 0 : designatedRouter.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractLANIdentifier<?> other = (AbstractLANIdentifier<?>) obj;
		if (designatedRouter == null) {
			if (other.designatedRouter != null)
				return false;
		} else if (!designatedRouter.equals(other.designatedRouter))
			return false;
		return true;
	}
}
