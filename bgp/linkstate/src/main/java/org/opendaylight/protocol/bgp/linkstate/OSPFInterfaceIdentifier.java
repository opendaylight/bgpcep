/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Arrays;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * An OSPFv3 interface identifier.
 * 
 * @see https://tools.ietf.org/html/draft-ietf-idr-ls-distribution-03#section-3.2.1.4
 */
public final class OSPFInterfaceIdentifier implements InterfaceIdentifier {
	private static final long serialVersionUID = 1L;
	private final byte[] value;

	public OSPFInterfaceIdentifier(final byte[] value) {
		Preconditions.checkNotNull(value);
		Preconditions.checkArgument(value.length == 4);
		this.value = value;
	}

	public byte[] getValue() {
		return this.value;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper.add("value", Arrays.toString(this.value));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.value);
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
		final OSPFInterfaceIdentifier other = (OSPFInterfaceIdentifier) obj;
		if (!Arrays.equals(this.value, other.value))
			return false;
		return true;
	}
}
