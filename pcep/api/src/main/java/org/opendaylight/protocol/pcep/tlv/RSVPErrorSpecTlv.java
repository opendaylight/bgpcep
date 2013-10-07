/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Structure of RSVP Error Spec Tlv.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc2205">Apendix A.5: ERROR_SPEC
 *      Class</a>
 * @param <T>
 */
public class RSVPErrorSpecTlv<T extends NetworkAddress<T>> implements Tlv {

	private final T errorNodeAddress;

	private final boolean inPlace;

	private final boolean guilty;

	private final int errorCode;

	private final int errorValue;

	/**
	 * 
	 * Constructs new RSVP Error Spec Tlv.
	 * 
	 * @param errorNodeAddress
	 *            T
	 * @param inPlace
	 *            boolean
	 * @param guilty
	 *            boolean
	 * @param errorCode
	 *            int
	 * @param errorValue
	 *            int
	 */
	public RSVPErrorSpecTlv(T errorNodeAddress, boolean inPlace, boolean guilty, int errorCode, int errorValue) {
		this.errorNodeAddress = errorNodeAddress;
		this.inPlace = inPlace;
		this.guilty = guilty;
		this.errorCode = errorCode;
		this.errorValue = errorValue;
	}

	/**
	 * Gets {@link NetworkAddress} of Error Node.
	 * 
	 * @return T
	 */
	public T getErrorNodeAddress() {
		return this.errorNodeAddress;
	}

	/**
	 * Setting of InPlace flag.
	 * 
	 * @return boolean
	 */
	public boolean isInPlace() {
		return this.inPlace;
	}

	/**
	 * Setting of Guilty flag.
	 * 
	 * @return boolean
	 */
	public boolean isGuilty() {
		return this.guilty;
	}

	/**
	 * Gets int representation of Error Code.
	 * 
	 * @return int
	 */
	public int getErrorCode() {
		return this.errorCode;
	}

	/**
	 * Gets int representation of Error Value.
	 * 
	 * @return int
	 */
	public int getErrorValue() {
		return this.errorValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.errorCode;
		result = prime * result + ((this.errorNodeAddress == null) ? 0 : this.errorNodeAddress.hashCode());
		result = prime * result + this.errorValue;
		result = prime * result + (this.guilty ? 1231 : 1237);
		result = prime * result + (this.inPlace ? 1231 : 1237);
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
		final RSVPErrorSpecTlv<?> other = (RSVPErrorSpecTlv<?>) obj;
		if (this.errorCode != other.errorCode)
			return false;
		if (this.errorNodeAddress == null) {
			if (other.errorNodeAddress != null)
				return false;
		} else if (!this.errorNodeAddress.equals(other.errorNodeAddress))
			return false;
		if (this.errorValue != other.errorValue)
			return false;
		if (this.guilty != other.guilty)
			return false;
		if (this.inPlace != other.inPlace)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RSVPErrorSpecTlv [errorNodeAddress=");
		builder.append(this.errorNodeAddress);
		builder.append(", inPlace=");
		builder.append(this.inPlace);
		builder.append(", guilty=");
		builder.append(this.guilty);
		builder.append(", errorCode=");
		builder.append(this.errorCode);
		builder.append(", errorValue=");
		builder.append(this.errorValue);
		builder.append("]");
		return builder.toString();
	}
}
