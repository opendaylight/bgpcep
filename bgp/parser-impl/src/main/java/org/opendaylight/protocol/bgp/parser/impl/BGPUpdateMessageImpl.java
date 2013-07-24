/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPObject;
import org.opendaylight.protocol.bgp.parser.BGPUpdateMessage;


/**
 * BGP Update Message.
 * 
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.3">BGP-4 Update Message Format</a>
 * 
 */
public class BGPUpdateMessageImpl implements BGPUpdateMessage {

	private static final long serialVersionUID = -1336770400381759349L;

	private final Set<BGPObject> addedObjects;

	private final Set<?> removedObjects;

	public BGPUpdateMessageImpl(final Set<BGPObject> addedObjects, final Set<?> removedObjects) {
		super();
		this.addedObjects = addedObjects;
		this.removedObjects = removedObjects;
	}

	@Override
	public Set<BGPObject> getAddedObjects() {
		return this.addedObjects;
	}

	@Override
	public Set<?> getRemovedObjects() {
		return this.removedObjects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.addedObjects == null) ? 0 : this.addedObjects.hashCode());
		result = prime * result + ((this.removedObjects == null) ? 0 : this.removedObjects.hashCode());
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
		final BGPUpdateMessageImpl other = (BGPUpdateMessageImpl) obj;
		if (this.addedObjects == null) {
			if (other.addedObjects != null)
				return false;
		} else if (!this.addedObjects.equals(other.addedObjects))
			return false;
		if (this.removedObjects == null) {
			if (other.removedObjects != null)
				return false;
		} else if (!this.removedObjects.equals(other.removedObjects))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("BGPUpdateMessageImpl [addedObjects=");
		builder.append(this.addedObjects);
		builder.append(", removedObjects=");
		builder.append(this.removedObjects);
		builder.append("]");
		return builder.toString();
	}
}
