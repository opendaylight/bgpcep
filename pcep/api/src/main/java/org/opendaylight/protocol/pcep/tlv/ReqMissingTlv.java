/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.pcep.PCEPTlv;

/**
 * Structure of Request Missing Tlv.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.15">PCEP-ERROR
 *      Object</a> - defined in text (Error-type=7)
 */
public class ReqMissingTlv implements PCEPTlv {
	private static final long serialVersionUID = -3910927830017195746L;
	private final long requestID;

	/**
	 * Constructs new Request Missing Tlv.
	 * 
	 * @param requestID
	 *            long
	 */
	public ReqMissingTlv(long requestID) {
		this.requestID = requestID;
	}

	/**
	 * gets long representation of Requested ID.
	 * 
	 * @return long
	 */
	public long getRequestID() {
		return this.requestID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.requestID ^ (this.requestID >>> 32));
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
		final ReqMissingTlv other = (ReqMissingTlv) obj;
		if (this.requestID != other.requestID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ReqMissingTlv [requestID=");
		builder.append(this.requestID);
		builder.append("]");
		return builder.toString();
	}

}
