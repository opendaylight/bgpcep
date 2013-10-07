/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP Reported Route Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.10">PCEP Reported Route Object</a>
 */
public class PCEPReportedRouteObject extends PCEPObject {

	private final List<ReportedRouteSubobject> subobjects;

	/**
	 * Constructs PCEP Reported Route Object.
	 * 
	 * @param subobjects List<ReportedRouteSubobject>. Can't be null or empty.
	 * @param processed boolean
	 */
	public PCEPReportedRouteObject(final List<ReportedRouteSubobject> subobjects, final boolean processed) {
		super(processed, false);
		if (subobjects == null || subobjects.isEmpty())
			throw new IllegalArgumentException("Subobjects can't be null or empty.");
		this.subobjects = subobjects;
	}

	/**
	 * Gets list of {@link ReportedRouteSubobject}.
	 * 
	 * @return List<PCEPSubobject>. Can't be null or empty.
	 */
	public List<ReportedRouteSubobject> getSubobjects() {
		return this.subobjects;
	}

	@Override
	public Boolean isIgnore() {
		return super.isIgnored();
	}

	@Override
	public Boolean isProcessingRule() {
		return super.isProcessed();
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("subobjects", this.subobjects);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.subobjects == null) ? 0 : this.subobjects.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPReportedRouteObject other = (PCEPReportedRouteObject) obj;
		if (this.subobjects == null) {
			if (other.subobjects != null)
				return false;
		} else if (!this.subobjects.equals(other.subobjects))
			return false;
		return true;
	}
}
