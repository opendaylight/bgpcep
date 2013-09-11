/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.util.List;

import org.opendaylight.protocol.framework.ProtocolMessage;

/**
 * Basic structure for PCEP Message. Cannot be instantiated directly. Current PCEP version is 1. Each message contains a
 * list of PCEP objects.
 * 
 */
public abstract class PCEPMessage implements ProtocolMessage {

	private static final long serialVersionUID = 4293319459468168384L;

	/**
	 * Current supported version of PCEP.
	 */
	public static final int PCEP_VERSION = 1;

	private final List<PCEPObject> objects;

	/**
	 * Constructor is protected to prevent direct instantiation, but to allow to call this constructor via super().
	 * 
	 * @param objects
	 */
	protected PCEPMessage(final List<PCEPObject> objects) {
		if (objects.contains(null))
			throw new IllegalArgumentException("Object list contains null element at offset " + objects.indexOf(null));

		this.objects = objects;
	}

	/**
	 * Returns list of all objects that the message contains
	 * 
	 * @return list of all objects that the message contains
	 */
	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.objects == null) ? 0 : this.objects.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPMessage other = (PCEPMessage) obj;
		if (this.objects == null) {
			if (other.objects != null)
				return false;
		} else if (!this.objects.equals(other.objects))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPMessage [objects=");
		builder.append(this.objects);
		builder.append("]");
		return builder.toString();
	}

}
