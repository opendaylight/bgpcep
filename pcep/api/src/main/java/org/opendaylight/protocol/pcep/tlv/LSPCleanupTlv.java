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
 * Structure of LSP Cleanup Tlv
 *
 * @see <a href="http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt#section-6.2.1">LSP-CLEANUP TLV</a>
 */
public class LSPCleanupTlv implements PCEPTlv {

	private static final long serialVersionUID = -2540695596612553355L;

	private final int timeout;

	/**
	 * Creates new LSP Cleanup Tlv.
	 *
	 */
	public LSPCleanupTlv(int timeout) {
		if (timeout < 0 || timeout > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Timeout (" + timeout + ") cannot be negative or bigger than 2^31 -1.");
		this.timeout = timeout;
	}

	/**
	 * @return the timeout
	 */
	public final int getTimeout() {
		return this.timeout;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.timeout;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LSPCleanupTlv))
			return false;
		final LSPCleanupTlv other = (LSPCleanupTlv) obj;
		if (this.timeout != other.timeout)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LSPCleanupTlv [timeout=");
		builder.append(this.timeout);
		builder.append("]");
		return builder.toString();
	}
}
