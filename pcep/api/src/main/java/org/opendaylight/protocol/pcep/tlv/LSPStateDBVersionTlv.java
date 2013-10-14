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
 * Structure of LSP State DB Version TLV.
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.1.2">LSP
 *      State Database Version TLV</a>
 */
public class LSPStateDBVersionTlv implements PCEPTlv {
	private static final long serialVersionUID = 3165807743418210453L;
	private final long dbVersion;

	/**
	 * Construct new LSP State DB Version TLV.
	 * 
	 * @param dbVersion
	 *            long
	 */
	public LSPStateDBVersionTlv(long dbVersion) {
		this.dbVersion = dbVersion;
	}

	/**
	 * Gets long representation of DB Version.
	 * 
	 * @return long
	 */
	public long getDbVersion() {
		return this.dbVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.dbVersion ^ (this.dbVersion >>> 32));
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
		final LSPStateDBVersionTlv other = (LSPStateDBVersionTlv) obj;
		if (this.dbVersion != other.dbVersion)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LSPStateDBVersionTlv [dbVersion=");
		builder.append(this.dbVersion);
		builder.append("]");
		return builder.toString();
	}
}
