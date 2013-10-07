/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Structure of LSP Symbolic Name Tlv.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.1">The
 *      LSP Symbolic Name TLV</a>
 */
public class LSPSymbolicNameTlv implements Tlv {
	private final LSPSymbolicName symbolicName;

	/**
	 * Constructs new LSP Symbolic Name TLV.
	 * 
	 * @param symbolicName
	 *            LSPSymbolicName
	 */
	public LSPSymbolicNameTlv(LSPSymbolicName symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Gets {@link LSPSymbolicName}.
	 * 
	 * @return LSPSymbolicName
	 */
	public LSPSymbolicName getSymbolicName() {
		return this.symbolicName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.symbolicName == null) ? 0 : this.symbolicName.hashCode());
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
		final LSPSymbolicNameTlv other = (LSPSymbolicNameTlv) obj;
		if (this.symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!this.symbolicName.equals(other.symbolicName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LSPSymbolicNameTlv [symbolicName=");
		builder.append(this.symbolicName);
		builder.append("]");
		return builder.toString();
	}

}
