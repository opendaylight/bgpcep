/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import org.opendaylight.protocol.concepts.ASNumber;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of Autonomous System Number Subobject.
 *
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.3.3.4">Section
 *      4.3.3.4.: Subobject 32: Autonomous System Number</a>
 */
public class EROAsNumberSubobject extends ExplicitRouteSubobject {

	private final ASNumber asnumber;

	/**
	 * Constructs new ASNumber Subobject.
	 *
	 * @param asnumber
	 *            ASNumber
	 * @param loose
	 *            boolean
	 */
	public EROAsNumberSubobject(ASNumber asnumber, boolean loose) {
		super(loose);
		this.asnumber = asnumber;
	}

	/**
	 * Gets {@link ASNumber}.
	 *
	 * @return ASNumber
	 */
	public ASNumber getASNumber() {
		return this.asnumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.asnumber == null) ? 0 : this.asnumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final EROAsNumberSubobject other = (EROAsNumberSubobject) obj;
		if (this.asnumber == null) {
			if (other.asnumber != null)
				return false;
		} else if (!this.asnumber.equals(other.asnumber))
			return false;
		return true;
	}

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("asnumber", this.asnumber);
		return super.addToStringAttributes(toStringHelper);
	}

}
