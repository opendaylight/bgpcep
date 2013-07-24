/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import org.opendaylight.protocol.concepts.NetworkAddress;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Abstract implementation of a NextHop class. This is a useful base class
 * implementing all the required semantics for a NextHop. Subclasses only
 * need to supply an appropriate public constructor.
 *
 * @param <T> template reference to subclass
 */
public abstract class AbstractNextHop<T extends NetworkAddress<T>> implements NextHop<T> {
	private static final long serialVersionUID = 2462286640242941943L;
	private final T global;
	private final T linkLocal;

	protected AbstractNextHop(final T global, final T linkLocal) {
		this.global = Preconditions.checkNotNull(global);
		this.linkLocal = linkLocal;
	}

	@Override
	public T getGlobal() {
		return global;
	}

	@Override
	public T getLinkLocal() {
		return linkLocal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((global == null) ? 0 : global.hashCode());
		result = prime * result
				+ ((linkLocal == null) ? 0 : linkLocal.hashCode());
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
		final AbstractNextHop<?> other = (AbstractNextHop<?>) obj;
		if (global == null) {
			if (other.global != null)
				return false;
		} else if (!global.equals(other.global))
			return false;
		if (linkLocal == null) {
			if (other.linkLocal != null)
				return false;
		} else if (!linkLocal.equals(other.linkLocal))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("global", global);
		toStringHelper.add("linkLocal", linkLocal);
		return toStringHelper;
	}

}
