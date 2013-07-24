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
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;

/**
 * Structure of Close Message.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.8">Close
 *      Message</a>
 */
public class PCEPCloseMessage extends PCEPMessage {

	private static final long serialVersionUID = -2365304678646268281L;

	private final PCEPCloseObject closeObj;

	/**
	 * Constructs a new Close Message, which has to include PCEP Close Object.
	 * Is used to close an established session between PCEP Peers.
	 *
	 * @throws IllegalArgumentException
	 *             if the CloseObject passed, is null.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.8">Close
	 *      Message</a>
	 *
	 * @param closeObj
	 *            Can't be null.
	 */
	public PCEPCloseMessage(final PCEPCloseObject closeObj) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = -8094080277421295531L;

			{
				if (closeObj != null)
					this.add(closeObj);
			}
		});
		if (closeObj == null)
			throw new IllegalArgumentException("PCEPCloseObject is mandatory. Can't be null.");

		this.closeObj = closeObj;
	}

	/**
	 * Gets {@link PCEPCloseObject}, which is mandatory object of PCEP Close
	 * Message.
	 *
	 * @return {@link PCEPCloseObject} . Can't be null.
	 */
	public PCEPCloseObject getCloseObject() {
		return this.closeObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.closeObj == null) ? 0 : this.closeObj.hashCode());
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
		final PCEPCloseMessage other = (PCEPCloseMessage) obj;
		if (this.closeObj == null) {
			if (other.closeObj != null)
				return false;
		} else if (!this.closeObj.equals(other.closeObj))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPCloseMessage [closeObj=");
		builder.append(this.closeObj);
		builder.append("]");
		return builder.toString();
	}
}
