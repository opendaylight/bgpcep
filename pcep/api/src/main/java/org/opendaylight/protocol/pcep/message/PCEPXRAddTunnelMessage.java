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
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;

/**
 * Add tunnel message for interop with EnXR PCC.
 */
public class PCEPXRAddTunnelMessage extends PCEPMessage {

	//private final PCEPRequestParameterObject requestParameter;

	private static final long serialVersionUID = -5832314519461628024L;

	private final PCEPLspObject lsp;

	private final PCEPEndPointsObject<?> endPoints;

	private final PCEPExplicitRouteObject ero;

	public PCEPXRAddTunnelMessage(final PCEPLspObject lsp,
			final PCEPEndPointsObject<?> endPoints, final PCEPExplicitRouteObject ero) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = -1810245662746464028L;

			{
				//if (requestParameter != null)
				//	this.add(requestParameter);
				if (lsp != null)
					this.add(lsp);
				if (endPoints != null)
					this.add(endPoints);
				if (ero != null)
					this.add(ero);
			}
		});
		if (lsp == null || endPoints == null || ero == null)
			throw new IllegalArgumentException("All objects are mandatory. Can't be null.");
	//	this.requestParameter = requestParameter;
		this.lsp = lsp;
		this.endPoints = endPoints;
		this.ero = ero;
	}

	/**
	 * @return the requestParameter
	 */
//	public PCEPRequestParameterObject getRequestParameter() {
//		return this.requestParameter;
//	}

	/**
	 * @return the lsp
	 */
	public PCEPLspObject getLsp() {
		return this.lsp;
	}

	/**
	 * @return the endPoints
	 */
	public PCEPEndPointsObject<?> getEndPoints() {
		return this.endPoints;
	}

	/**
	 * @return the ero
	 */
	public PCEPExplicitRouteObject getEro() {
		return this.ero;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.endPoints == null) ? 0 : this.endPoints.hashCode());
		result = prime * result + ((this.ero == null) ? 0 : this.ero.hashCode());
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
		if (obj == null)
			return false;
		if (!(obj instanceof PCEPXRAddTunnelMessage))
			return false;
		final PCEPXRAddTunnelMessage other = (PCEPXRAddTunnelMessage) obj;
		if (this.endPoints == null) {
			if (other.endPoints != null)
				return false;
		} else if (!this.endPoints.equals(other.endPoints))
			return false;
		if (this.ero == null) {
			if (other.ero != null)
				return false;
		} else if (!this.ero.equals(other.ero))
			return false;
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
		builder.append("PCEPXRAddTunnelMessage [lsp=");
		builder.append(this.lsp);
		builder.append(", endPoints=");
		builder.append(this.endPoints);
		builder.append(", ero=");
		builder.append(this.ero);
		builder.append("]");
		return builder.toString();
	}
}
