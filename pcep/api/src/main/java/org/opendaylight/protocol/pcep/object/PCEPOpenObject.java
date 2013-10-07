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
 * Structure of PCEP Open Object.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.3">PCEP Open Object</a>
 */
public class PCEPOpenObject extends PCEPObject {

	public static final int PCEP_VERSION = 1;

	private final int keepAliveTimerValue;

	private final int deadTimerValue;

	private final int sessionId;

	private final List<Tlv> tlvs;

	/**
	 * Constructs PCEP Open Object also with optional Objects.
	 * 
	 * @param keepAliveTimerValue int
	 * @param deadTimerValue int
	 * @param sessionId int
	 * @param tlvs List<PCEPTlv>
	 */
	public PCEPOpenObject(final int keepAliveTimerValue, final int deadTimerValue, final int sessionId, final List<Tlv> tlvs) {
		super(false, false);
		this.keepAliveTimerValue = keepAliveTimerValue;
		this.deadTimerValue = deadTimerValue;
		this.sessionId = sessionId;
		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * Constructs PCEP Open Object only with mandatory values.
	 * 
	 * @param keepAliveTimerValue int
	 * @param deadTimerValue int
	 * @param sessionId int
	 */
	public PCEPOpenObject(final int keepAliveTimerValue, final int deadTimerValue, final int sessionId) {
		this(keepAliveTimerValue, deadTimerValue, sessionId, Collections.<Tlv> emptyList());
	}

	/**
	 * Returns integer representation of Keep Alive Timer.
	 * 
	 * @return int
	 */
	public int getKeepAliveTimerValue() {
		return this.keepAliveTimerValue;
	}

	/**
	 * Returns integer representation of Dead Timer.
	 * 
	 * @return int
	 */
	public int getDeadTimerValue() {
		return this.deadTimerValue;
	}

	/**
	 * Returns integer representation of Session ID.
	 * 
	 * @return int
	 */
	public int getSessionId() {
		return this.sessionId;
	}

	/**
	 * Gets list of {@link PCEPTlv}.
	 * 
	 * @return List<PCEPTlv>
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
		int result = 1;
		result = prime * result + this.deadTimerValue;
		result = prime * result + this.keepAliveTimerValue;
		result = prime * result + this.sessionId;
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
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
		final PCEPOpenObject other = (PCEPOpenObject) obj;
		if (this.deadTimerValue != other.deadTimerValue)
			return false;
		if (this.keepAliveTimerValue != other.keepAliveTimerValue)
			return false;
		if (this.sessionId != other.sessionId)
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
		toStringHelper.add("keepAliveTimerValue", this.keepAliveTimerValue);
		toStringHelper.add("deadTimerValue", this.deadTimerValue);
		toStringHelper.add("sessionId", this.sessionId);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
