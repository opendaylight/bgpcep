/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.opendaylight.protocol.framework.SessionPreferences;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 * Implementation of {@link SessionPreferences}.
 */
public final class PCEPSessionPreferences implements SessionPreferences {

	private final PCEPOpenObject openObject;

	/**
	 * Construct new session preferences.
	 *
	 * @param openObject encapsulated PCEP OPEN object
	 */
	public PCEPSessionPreferences(final PCEPOpenObject openObject) {
		this.openObject = openObject;
	}

	/**
	 * Return the encapsulated OPEN object.
	 *
	 * @return encapsulated OPEN object.
	 */
	public PCEPOpenObject getOpenObject() {
		return this.openObject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.openObject == null) ? 0 : this.openObject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PCEPSessionPreferences))
			return false;
		final PCEPSessionPreferences other = (PCEPSessionPreferences) obj;
		if (this.openObject == null) {
			if (other.openObject != null)
				return false;
		} else if (!this.openObject.equals(other.openObject))
			return false;
		return true;
	}
}
