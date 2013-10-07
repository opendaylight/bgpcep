/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure of Update Request Message.
 * 
 * @see <a href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-6.2">Update Request Message</a>
 */
public class PCEPUpdateRequestMessage implements Message {

	private final List<CompositeUpdateRequestObject> updateRequests;

	private final List<PCEPObject> objects;

	/**
	 * Constructs new Update Request Message.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeUpdateRequestObject} in the list.
	 * 
	 * @param updateRequests List<CompositeUpdateRequestObject>. Can't be empty or null.
	 */
	public PCEPUpdateRequestMessage(final List<CompositeUpdateRequestObject> updateRequests) {
		this.objects = Lists.newArrayList();
		if (updateRequests != null)
			for (final CompositeUpdateRequestObject curo : updateRequests) {
				this.objects.addAll(curo.getCompositeAsList());
			}

		if (updateRequests == null || updateRequests.isEmpty())
			throw new IllegalArgumentException("At least one CompositeUpdateRequestObject is mandatory.");
		this.updateRequests = updateRequests;
	}

	/**
	 * Gets list of {@link CompositeUpdateRequestObject}.
	 * 
	 * @return List<CompositeUpdateRequestObject>. Can't be null or empty.
	 */
	public List<CompositeUpdateRequestObject> getUpdateRequests() {
		return this.updateRequests;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.updateRequests == null) ? 0 : this.updateRequests.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPUpdateRequestMessage other = (PCEPUpdateRequestMessage) obj;
		if (this.updateRequests == null) {
			if (other.updateRequests != null)
				return false;
		} else if (!this.updateRequests.equals(other.updateRequests))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPUpdateRequestMessage [updateRequests=");
		builder.append(this.updateRequests);
		builder.append("]");
		return builder.toString();
	}
}
