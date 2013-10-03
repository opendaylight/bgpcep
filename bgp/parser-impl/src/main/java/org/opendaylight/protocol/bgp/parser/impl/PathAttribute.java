/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import org.omg.CORBA.TypeCode;

/**
 * Path Attribute Object defines attributes to routes that are advertised through BGP. Each Attribute is a triplet
 * <type, length, value>.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.3">BGP-4</a>
 * 
 */
public final class PathAttribute {

	// Attribute type -------------------------------------------------------
	/**
	 * Size of the flags field in path attribute, in bytes.
	 */
	public static final int ATTR_FLAGS_SIZE = 1;

	/**
	 * 0 - Optional bit: attribute is optional (if set to 1) or well-known (if set to 0)
	 */
	private final boolean optional;

	/**
	 * 1 - Transitive bit: attribute is transitive (if set to 1) or non-transitive (if set to 0)
	 */
	private final boolean transitive;

	/**
	 * 2 - Partial bit: attribute is partial (if set to 1) or complete (if set to 0)
	 */
	private final boolean partial;

	/**
	 * 3 - Extended Length bit: attribute length is one octet (if set to 0) or two octets (if set to 1)
	 */
	private final boolean extendedLength;

	/**
	 * Size of the field Attribute Type Code, in bytes.
	 */
	public static final int ATTR_TYPE_CODE_SIZE = 1;

	private TypeCode type;

	// Attribute Length ------------------------------------------------------

	/**
	 * Size of the attribute length field, in bytes. Depends on extendedLengthBit.
	 */
	private final int attrLengthSize;

	/**
	 * Length of the attribute value, in bytes.
	 */
	private int length;

	// -----------------------------------------------------------------------

	/**
	 * Attribute value
	 */
	private Object value;

	// Constructors ----------------------------------------------------------

	public PathAttribute(final boolean optional, final boolean transitive, final boolean partial, final boolean extendedLength) {
		this(null, optional, transitive, partial, extendedLength, null);
	}

	public PathAttribute(final TypeCode type, final boolean optional, final boolean transitive, final boolean partial,
			final boolean extendedLength, final Object value) {
		this.type = type;
		this.optional = optional;
		this.transitive = transitive;
		this.partial = partial;
		this.extendedLength = extendedLength;
		this.value = value;
		this.attrLengthSize = (this.extendedLength) ? 2 : 1;
	}

	// Getters & setters -----------------------------------------------------

	public TypeCode getType() {
		return this.type;
	}

	public int getLength() {
		return this.length;
	}

	public Object getValue() {
		return this.value;
	}

	public int getAttrLengthSize() {
		return this.attrLengthSize;
	}

	public void setType(final TypeCode type) {
		this.type = type;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public void setLength(final int length) {
		this.length = length;
	}

	public boolean isOptional() {
		return this.optional;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PathAttribute [optional=");
		builder.append(this.optional);
		builder.append(", transitive=");
		builder.append(this.transitive);
		builder.append(", partial=");
		builder.append(this.partial);
		builder.append(", extendedLength=");
		builder.append(this.extendedLength);
		builder.append(", type=");
		builder.append(this.type);
		builder.append(", attrLengthSize=");
		builder.append(this.attrLengthSize);
		builder.append(", length=");
		builder.append(this.length);
		builder.append(", value=");
		builder.append(this.value);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.attrLengthSize;
		result = prime * result + (this.extendedLength ? 1231 : 1237);
		result = prime * result + (this.optional ? 1231 : 1237);
		result = prime * result + (this.partial ? 1231 : 1237);
		result = prime * result + (this.transitive ? 1231 : 1237);
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
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
		final PathAttribute other = (PathAttribute) obj;
		if (this.attrLengthSize != other.attrLengthSize)
			return false;
		if (this.extendedLength != other.extendedLength)
			return false;
		if (this.optional != other.optional)
			return false;
		if (this.partial != other.partial)
			return false;
		if (this.transitive != other.transitive)
			return false;
		if (this.type != other.type)
			return false;
		if (this.value == null) {
			if (other.value != null)
				return false;
		} else if (!this.value.equals(other.value))
			return false;
		return true;
	}
}
