/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Basic structure for PCEP Objects.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.2">Common Object Header</a>
 */
public abstract class PCEPObject implements Object {

	private final boolean processed;

	private final boolean ignored;

	/**
	 * Constructor is protected to prevent direct instantiation, but to allow to call this constructor via super().
	 * 
	 * @param processed P flag
	 * @param ignored I flag
	 */
	protected PCEPObject(final boolean processed, final boolean ignored) {
		this.processed = processed;
		this.ignored = ignored;
	}

	/**
	 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.2"> Common Object Header</a>
	 * 
	 * @return true if P flag is set and false if is not.
	 */
	public boolean isProcessed() {
		return this.processed;
	}

	/**
	 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.2"> Common Object Header</a>
	 * 
	 * @return true if I flag is set and false if is not.
	 */
	public boolean isIgnored() {
		return this.ignored;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.ignored ? 1231 : 1237);
		result = prime * result + (this.processed ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(final java.lang.Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPObject other = (PCEPObject) obj;
		if (this.ignored != other.ignored)
			return false;
		if (this.processed != other.processed)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("processed", this.processed);
		toStringHelper.add("ignored", this.ignored);
		return toStringHelper;
	}

}
