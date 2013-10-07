/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Structure of LSP Update Error TLV.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.3">LSP
 *      Update Error Code TLV</a>
 */
public class LSPUpdateErrorTlv implements Tlv {
	private final byte[] errorCode;

	/**
	 * Constructs new LSP Update Error Tlv.
	 * 
	 * @param errorCode
	 *            byte[]. Size has to be 4 bytes.
	 */
	public LSPUpdateErrorTlv(byte[] errorCode) {
		if (errorCode.length != 4)
			throw new IllegalArgumentException("Update error code has wrong size.");
		this.errorCode = errorCode;
	}

	/**
	 * TBD
	 * 
	 * @return error code as byte[]
	 */
	public byte[] getErrorCode() {
		return this.errorCode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.errorCode);
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
		final LSPUpdateErrorTlv other = (LSPUpdateErrorTlv) obj;
		if (!Arrays.equals(this.errorCode, other.errorCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LSPUpdateErrorTlv [errorCode=");
		builder.append(Arrays.toString(this.errorCode));
		builder.append("]");
		return builder.toString();
	}

}
