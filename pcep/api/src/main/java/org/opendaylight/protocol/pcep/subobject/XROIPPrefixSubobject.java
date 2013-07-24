/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.protocol.concepts.Prefix;

/**
 * Parametrized structure of IP Prefix Subobject. Defined in RFC5521.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1">Exclude Route
 *      Object definition</a>
 *
 * @param <T>
 *            subtype of Prefix
 */
public class XROIPPrefixSubobject<T extends Prefix<?>> extends ExcludeRouteSubobject {

	private final XROSubobjectAttribute attribute;

	private final T prefix;

	/**
	 * Constructs IPPrefix Subobject.
	 *
	 * @param prefix
	 *            T
	 * @param mandatory
	 *            boolean
	 * @param attribute
	 *            XROSubobjectAttribute
	 */
	public XROIPPrefixSubobject(T prefix, boolean mandatory, XROSubobjectAttribute attribute) {
		super(mandatory);
		this.attribute = attribute;
		this.prefix = prefix;
	}

	/**
	 * Gets specific {@link Prefix}.
	 *
	 * @return prefix T
	 */
	public T getPrefix() {
		return this.prefix;
	}

	/**
	 * Gets the attribute of the subobject
	 *
	 * @return the attribute
	 */
	public XROSubobjectAttribute getAttribute() {
		return this.attribute;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.attribute == null) ? 0 : this.attribute.hashCode());
		result = prime * result + ((this.prefix == null) ? 0 : this.prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final XROIPPrefixSubobject<?> other = (XROIPPrefixSubobject<?>) obj;
		if (this.attribute != other.attribute)
			return false;
		if (this.prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!this.prefix.equals(other.prefix))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("XROIPPrefixSubobject [attribute=");
		builder.append(this.attribute);
		builder.append(", prefix=");
		builder.append(this.prefix);
		builder.append(", mandatory=");
		builder.append(this.mandatory);
		builder.append("]");
		return builder.toString();
	}

}
