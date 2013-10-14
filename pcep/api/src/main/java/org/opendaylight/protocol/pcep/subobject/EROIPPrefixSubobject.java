/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Parametrized structure of IP Prefix Subobject.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.3.3.2">Section 4.3.3.2.: Subobject 1: IPv4 prefix</a> and
 *      <a href="http://tools.ietf.org/html/rfc3209#section-4.3.3.3">Section 4.3.3.2.: Subobject 2: IPv6 prefix</a>
 * 
 * @param <T> subtype of Prefix
 */
public class EROIPPrefixSubobject extends ExplicitRouteSubobject {

	private final IpPrefix prefix;

	/**
	 * Constructs IPPrefix Subobject.
	 * 
	 * @param prefix T
	 * @param loose boolean
	 */
	public EROIPPrefixSubobject(final IpPrefix prefix, final boolean loose) {
		super(loose);
		this.prefix = prefix;
	}

	/**
	 * Gets specific {@link Prefix}.
	 * 
	 * @return prefix T
	 */
	public IpPrefix getPrefix() {
		return this.prefix;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.prefix == null) ? 0 : this.prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final EROIPPrefixSubobject other = (EROIPPrefixSubobject) obj;
		if (this.prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!this.prefix.equals(other.prefix))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("prefix", this.prefix);
		return super.addToStringAttributes(toStringHelper);
	}

}
