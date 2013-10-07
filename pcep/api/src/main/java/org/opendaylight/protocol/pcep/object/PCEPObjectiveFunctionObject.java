/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Indicates the desired/required objective function to be applied by the PCE during path computation or within a PCRep
 * message so as to indicate the objective function that was used by the PCE during path computation.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5541#section-3.1">OF Object</a>
 */
public class PCEPObjectiveFunctionObject extends PCEPObject {

	private final PCEPOFCodes code;

	private final List<PCEPTlv> tlvs;

	/**
	 * Constructs objective function object only with mandatory objects.
	 * 
	 * @param code PCEPOFCodes
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPObjectiveFunctionObject(final PCEPOFCodes code, final boolean processed, final boolean ignored) {
		this(code, null, processed, ignored);
	}

	/**
	 * Constructs objective function object also with optional objects.
	 * 
	 * @param code PCEPOFCodes
	 * @param tlvs the list of tlvs
	 * @param processed boolean
	 * @param ignored boolean
	 */
	public PCEPObjectiveFunctionObject(final PCEPOFCodes code, final List<PCEPTlv> tlvs, final boolean processed, final boolean ignored) {
		super(processed, ignored);
		this.code = code;

		if (tlvs == null)
			this.tlvs = Collections.emptyList();
		else
			this.tlvs = tlvs;
	}

	/**
	 * Gets the objective function code
	 * 
	 * @return the PCEPOFCodes
	 */
	public PCEPOFCodes getCode() {
		return this.code;
	}

	/**
	 * Gets the list of tlvs
	 * 
	 * @return the list of tlvs
	 */
	public List<PCEPTlv> getTlvs() {
		return this.tlvs;
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
		toStringHelper.add("code", this.code);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.code == null) ? 0 : this.code.hashCode());
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
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
		final PCEPObjectiveFunctionObject other = (PCEPObjectiveFunctionObject) obj;
		if (this.code != other.code)
			return false;
		if (this.tlvs == null) {
			if (other.tlvs != null)
				return false;
		} else if (!this.tlvs.equals(other.tlvs))
			return false;
		return true;
	}

}
