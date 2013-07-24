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
import org.opendaylight.protocol.pcep.PCEPTlv;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of Close Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.17">PCEP Close
 *      Object</a>
 */
public class PCEPCloseObject extends PCEPObject {

	/**
	 * Constants for reasons of closing session.
	 */
	public enum Reason {
		UNKNOWN, EXP_DEADTIMER, MALFORMED_MSG, TOO_MANY_UNKNOWN_REQ_REP, TOO_MANY_UNKNOWN_MSG
	}

	private final Reason reason;

	private List<PCEPTlv> tlvs;

	/**
	 * Constructs Close Object only with mandatory object.
	 *
	 * @param reason
	 *            Reason. Can't be null.
	 */
	public PCEPCloseObject(Reason reason) {
		this(reason, null);
	}

	/**
	 * Constructs Close Object also with optional objects.
	 *
	 * @param reason
	 *            Reason. Can't be null.
	 * @param tlvs
	 *            List<PCEPTlv>
	 */
	public PCEPCloseObject(Reason reason, List<PCEPTlv> tlvs) {
		super(false, false);
		if (reason == null)
			throw new IllegalArgumentException("Reason is mandatory.");
		this.reason = reason;

		if (tlvs != null)
			this.tlvs = tlvs;
		else
			this.tlvs = Collections.emptyList();
	}

	/**
	 * Gets {@link Reason}
	 *
	 * @return Reason. Can't be null.
	 */
	public Reason getReason() {
		return this.reason;
	}

	/**
	 * Gets list of {@link PCEPTlv}
	 *
	 * @return List<PCEPTlv>. Can't be null, but may be empty.
	 */
	public List<PCEPTlv> getTlvs() {
		return this.tlvs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.reason == null) ? 0 : this.reason.hashCode());
		result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
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
		final PCEPCloseObject other = (PCEPCloseObject) obj;
		if (this.reason != other.reason)
			return false;
		if (this.tlvs == null) {
			if (other.tlvs != null)
				return false;
		} else if (!this.tlvs.equals(other.tlvs))
			return false;
		return true;
	}

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("reason", this.reason);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}

}
