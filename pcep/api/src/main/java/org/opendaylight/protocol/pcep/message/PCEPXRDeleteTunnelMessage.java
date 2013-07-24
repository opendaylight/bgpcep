/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.ArrayList;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;

/**
 * Delete tunnel message for interop with EnXR PCC.
 */
public class PCEPXRDeleteTunnelMessage extends PCEPMessage {

	private static final long serialVersionUID = -8147187272108419351L;

	private final PCEPLspObject lsp;

	public PCEPXRDeleteTunnelMessage(final PCEPLspObject lsp) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = 3374457164202667362L;

			{
				if (lsp != null)
					this.add(lsp);
			}
		});
		if (lsp == null)
			throw new IllegalArgumentException("All objects are mandatory. Can't be null.");
		this.lsp = lsp;
	}

	/**
	 * @return the lsp
	 */
	public PCEPLspObject getLsp() {
		return this.lsp;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.lsp == null) ? 0 : this.lsp.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof PCEPXRDeleteTunnelMessage))
			return false;
		final PCEPXRDeleteTunnelMessage other = (PCEPXRDeleteTunnelMessage) obj;
		if (this.lsp == null) {
			if (other.lsp != null)
				return false;
		} else if (!this.lsp.equals(other.lsp))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPXRDeleteTunnelMessage [lsp=");
		builder.append(this.lsp);
		builder.append("]");
		return builder.toString();
	}
}
