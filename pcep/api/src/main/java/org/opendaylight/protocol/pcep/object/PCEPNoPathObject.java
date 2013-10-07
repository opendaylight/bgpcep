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

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP No Path Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.5">PCEP No Path Object</a>
 */
public class PCEPNoPathObject extends PCEPObject {

	private final short natureOfIssue;

	private final boolean constrained;

	private final List<Tlv> tlvs;

	/**
	 * Constructs PCEP No Path Object only with mandatory values.
	 * 
	 * @param natureOfIssue short
	 * @param constrained boolean
	 * @param ignored boolean
	 */
	public PCEPNoPathObject(final short natureOfIssue, final boolean constrained, final boolean ignored) {
		this(natureOfIssue, constrained, null, ignored);
	}

	/**
	 * Constructs PCEP No Path Object also with optional Objects.
	 * 
	 * @param natureOfIssue short
	 * @param constrained boolean
	 * @param tlvs List<PCEPTlv>
	 * @param ignored boolean
	 */
	public PCEPNoPathObject(final short natureOfIssue, final boolean constrained, final List<Tlv> tlvs, final boolean ignored) {
		super(false, ignored);
		this.natureOfIssue = natureOfIssue;
		this.constrained = constrained;
		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * Returns short representation of Nature of issue.
	 * 
	 * @return short
	 */
	public short getNatureOfIssue() {
		return this.natureOfIssue;
	}

	/**
	 * Gets Constrained flag.
	 * 
	 * @return boolean
	 */
	public boolean isConstrained() {
		return this.constrained;
	}

	/**
	 * Gets list of {@link PCEPTlv}
	 * 
	 * @return List<PCEPTlv>. Can't be null, but may be empty.
	 */
	public List<Tlv> getTlvs() {
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
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (this.constrained ? 1231 : 1237);
		result = prime * result + this.natureOfIssue;
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
		final PCEPNoPathObject other = (PCEPNoPathObject) obj;
		if (this.constrained != other.constrained)
			return false;
		if (this.natureOfIssue != other.natureOfIssue)
			return false;
		if (this.tlvs == null) {
			if (other.tlvs != null)
				return false;
		} else if (!this.tlvs.equals(other.tlvs))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("natureOfIssue", this.natureOfIssue);
		toStringHelper.add("constrained", this.constrained);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
