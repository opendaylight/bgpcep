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
 * Structure of PCEP Notification Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.14">PCEP Notification Object</a>
 */
public class PCEPNotificationObject extends PCEPObject {

	private final short type;

	private final short value;

	private List<Tlv> tlvs;

	/**
	 * Constructs PCEP Notification Object only with mandatory values.
	 * 
	 * @param type short
	 * @param value short
	 */
	public PCEPNotificationObject(final short type, final short value) {
		this(type, value, null);
	}

	/**
	 * Constructs PCEP Notification Object also with optional Objects.
	 * 
	 * @param type short
	 * @param value short
	 * @param tlvs List<PCEPTlv>
	 */
	public PCEPNotificationObject(final short type, final short value, final List<Tlv> tlvs) {
		super(false, false);
		this.type = type;
		this.value = value;
		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * Returns short representation of Type.
	 * 
	 * @return short
	 */
	public short getType() {
		return this.type;
	}

	/**
	 * Returns short value.
	 * 
	 * @return short
	 */
	public short getValue() {
		return this.value;
	}

	/**
	 * Gets list of {@link Tlv}
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
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
		result = prime * result + this.type;
		result = prime * result + this.value;
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
		final PCEPNotificationObject other = (PCEPNotificationObject) obj;
		if (this.tlvs == null) {
			if (other.tlvs != null)
				return false;
		} else if (!this.tlvs.equals(other.tlvs))
			return false;
		if (this.type != other.type)
			return false;
		if (this.value != other.value)
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("type", this.type);
		toStringHelper.add("value", this.value);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
