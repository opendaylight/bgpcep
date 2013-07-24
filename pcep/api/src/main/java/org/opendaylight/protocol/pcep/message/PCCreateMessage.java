/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.CompositeInstantiationObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;

/**
 * @see <a href="http://www.ietf.org/id/draft-crabbe-pce-pce-initiated-lsp-00.txt">5.1.  The LSP Create Message</a>
 */
public class PCCreateMessage extends PCEPMessage {

	private static final long serialVersionUID = -2407399568579836171L;

	private final List<CompositeInstantiationObject> lsps;

	/**
	 * Constructs {@link PCCreateMessage}.
	 *
	 * @throws IllegalArgumentException
	 *             if there is not even one {@link CompositeInstantiationObject}
	 *             in the list.
	 *
	 * @param lsps
	 *            List<CompositeInstantiationObject>. Can't be empty or null.
	 */
	public PCCreateMessage(final List<CompositeInstantiationObject> lsps) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = 1L;

			{
				if (lsps != null)
					for (final CompositeInstantiationObject cio : lsps) {
						this.addAll(cio.getCompositeAsList());
					}
			}
		});
		if (lsps == null || lsps.isEmpty())
			throw new IllegalArgumentException("At least one CompositeStateReportObject is mandatory.");

		this.lsps = lsps;
	}

	/**
	 * Gets list of {@link CompositeStateReportObject}.
	 *
	 * @return List<CompositeStateReportObject>. Can't be null or empty.
	 */
	public List<CompositeInstantiationObject> getLSPs() {
		return this.lsps;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.lsps == null) ? 0 : this.lsps.hashCode());
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
		if (!(obj instanceof PCCreateMessage))
			return false;
		final PCCreateMessage other = (PCCreateMessage) obj;
		if (this.lsps == null) {
			if (other.lsps != null)
				return false;
		} else if (!this.lsps.equals(other.lsps))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCCreateMessage [lsps=");
		builder.append(this.lsps);
		builder.append("]");
		return builder.toString();
	}
}
