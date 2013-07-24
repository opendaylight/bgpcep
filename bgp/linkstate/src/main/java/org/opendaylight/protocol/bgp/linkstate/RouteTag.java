/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.io.Serializable;
import java.util.Arrays;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.primitives.SignedBytes;

public final class RouteTag implements Serializable, Comparable<RouteTag> {
	private static final long serialVersionUID = 1L;
	private final byte[] value;

	public RouteTag(final byte[] value) {
		Preconditions.checkNotNull(value);
		Preconditions.checkArgument(value.length == 4);
		this.value = value;
	}

	@Override
	public String toString(){
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("value", value);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(value);
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
		RouteTag other = (RouteTag) obj;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	@Override
	public int compareTo(RouteTag o) {
		if (o == null)
			return 1;
		if (o == this)
			return 0;
		return SignedBytes.lexicographicalComparator().compare(value, o.value);
	}
}
