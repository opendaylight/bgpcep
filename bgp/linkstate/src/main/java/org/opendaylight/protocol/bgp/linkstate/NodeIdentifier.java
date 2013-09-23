/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * A network node identifier. A network node is typically a router, a switch, or similar entity.
 */
public final class NodeIdentifier implements Identifier {
	private static final long serialVersionUID = 1L;

	private final AsNumber asNumber;
	private final DomainIdentifier domainIdentifier;
	private final AreaIdentifier areaIdentifier;
	private final RouterIdentifier routerIdentifier;

	public NodeIdentifier(final AsNumber as, final DomainIdentifier domain, final AreaIdentifier area, final RouterIdentifier router) {
		this.routerIdentifier = Preconditions.checkNotNull(router, "Router Identifier is mandatory.");
		this.asNumber = as;
		this.domainIdentifier = domain;
		this.areaIdentifier = area;
	}

	/**
	 * Return the AS number where this node resides.
	 * 
	 * @return AS of residence
	 */
	public AsNumber getAsNumber() {
		return this.asNumber;
	}

	public DomainIdentifier getDomainIdentifier() {
		return this.domainIdentifier;
	}

	public AreaIdentifier getAreaIdentifier() {
		return this.areaIdentifier;
	}

	public RouterIdentifier getRouterIdentifier() {
		return this.routerIdentifier;
	}

	@Override
	public String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("as", this.asNumber);
		toStringHelper.add("domain", this.domainIdentifier);
		toStringHelper.add("area", this.areaIdentifier);
		toStringHelper.add("router", this.routerIdentifier);
		return toStringHelper;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.areaIdentifier == null) ? 0 : this.areaIdentifier.hashCode());
		result = prime * result + ((this.asNumber == null) ? 0 : this.asNumber.hashCode());
		result = prime * result + ((this.domainIdentifier == null) ? 0 : this.domainIdentifier.hashCode());
		result = prime * result + ((this.routerIdentifier == null) ? 0 : this.routerIdentifier.hashCode());
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
		final NodeIdentifier other = (NodeIdentifier) obj;
		if (this.areaIdentifier == null) {
			if (other.areaIdentifier != null)
				return false;
		} else if (!this.areaIdentifier.equals(other.areaIdentifier))
			return false;
		if (this.asNumber == null) {
			if (other.asNumber != null)
				return false;
		} else if (!this.asNumber.equals(other.asNumber))
			return false;
		if (this.domainIdentifier == null) {
			if (other.domainIdentifier != null)
				return false;
		} else if (!this.domainIdentifier.equals(other.domainIdentifier))
			return false;
		if (this.routerIdentifier == null) {
			if (other.routerIdentifier != null)
				return false;
		} else if (!this.routerIdentifier.equals(other.routerIdentifier))
			return false;
		return true;
	}
}
