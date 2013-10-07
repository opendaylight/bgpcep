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
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Provides a list of network resources that the PCE is requested to exclude from the path that it computes. Flags
 * associated with each list member instruct the PCE as to whether the network resources must be excluded from the
 * computed path, or whether the PCE should make best efforts to exclude the resources from the computed path.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1"> Exclude Route Object definition</a>
 */
public class PCEPExcludeRouteObject extends PCEPObject {

	private final boolean fail;

	private final List<ExcludeRouteSubobject> subobjects;

	/**
	 * Constructs Exclude Route Object.
	 * 
	 * @param subobjects List<PCEPXROSubobject>. Can't be null or empty.
	 * @param fail boolean
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPExcludeRouteObject(final List<ExcludeRouteSubobject> subobjects, final boolean fail, final boolean processed,
			final boolean ignored) {
		super(processed, ignored);
		if (subobjects == null || subobjects.isEmpty())
			throw new IllegalArgumentException("Subobjects can't be null or empty.");

		this.fail = fail;
		this.subobjects = subobjects;
	}

	/**
	 * Gets list of sub-objects
	 * 
	 * @return List<PCEPXROSubobject>. Can't be null or empty.
	 */
	public List<ExcludeRouteSubobject> getSubobjects() {
		return this.subobjects;
	}

	/**
	 * @see <a href="http://tools.ietf.org/html/rfc5521#section-2.1.1"> Exclude Route Object definition</a>
	 * 
	 * @return if returns true, the requesting PCC requires the computation of a new path for an existing TE LSP that
	 *         has failed
	 */
	public boolean isFail() {
		return this.fail;
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
		toStringHelper.add("fail", this.fail);
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
		final PCEPExcludeRouteObject other = (PCEPExcludeRouteObject) obj;
		if (this.subobjects == null) {
			if (other.subobjects != null)
				return false;
		} else if (!this.subobjects.equals(other.subobjects))
			return false;
		return true;
	}

}
