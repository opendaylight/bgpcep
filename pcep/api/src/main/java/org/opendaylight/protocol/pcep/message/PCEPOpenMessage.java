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
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 * Structure of Open Message.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.2">Open
 *      Message</a>
 */
public class PCEPOpenMessage extends PCEPMessage {

	private static final long serialVersionUID = -588927926753235030L;

	private final PCEPOpenObject openObj;

	/**
	 * Constructs new Open Message.
	 *
	 * @throws IllegalArgumentException
	 *             if the PCEPOpenObject is null.
	 *
	 * @param openObj
	 *            {@link PCEPOpenObject}. Can't be null.
	 */
	public PCEPOpenMessage(final PCEPOpenObject openObj) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = -1339062869814655362L;

			{
				if (openObj != null)
					this.add(openObj);
			}
		});

		if (openObj == null)
			throw new IllegalArgumentException("PCEPOpenObject is mandatory.");

		this.openObj = openObj;
	}

	/**
	 * Gets {@link PCEPOpenObject}
	 *
	 * @return {@link PCEPOpenObject}. Can't be null.
	 */
	public PCEPOpenObject getOpenObject() {
		return this.openObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.openObj == null) ? 0 : this.openObj.hashCode());
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
		final PCEPOpenMessage other = (PCEPOpenMessage) obj;
		if (this.openObj == null) {
			if (other.openObj != null)
				return false;
		} else if (!this.openObj.equals(other.openObj))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPOpenMessage [openObj=");
		builder.append(this.openObj);
		builder.append("]");
		return builder.toString();
	}
}
