/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestSvecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure of Request Message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.4">Request Message</a>
 */
public class PCEPRequestMessage implements Message {

	private final List<CompositeRequestSvecObject> svecList;

	private final List<CompositeRequestObject> requests;

	private final List<PCEPObject> objects;

	/**
	 * Constructs new Request Message.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeRequestObject} in the list.
	 * 
	 * @param requests List<CompositeRequestObject>. Can't be empty or null.
	 */
	public PCEPRequestMessage(final List<CompositeRequestObject> requests) {
		this(null, requests);
	}

	/**
	 * Constructs new Request Message.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeRequestObject} in the list.
	 * 
	 * @param svecList List<CompositeSvecObject>
	 * @param requests List<CompositeRequestObject>. Can't be null or empty.
	 */
	public PCEPRequestMessage(final List<CompositeRequestSvecObject> svecList, final List<CompositeRequestObject> requests) {
		this.objects = Lists.newArrayList();
		if (svecList != null)
			for (final CompositeRequestSvecObject cso : svecList) {
				this.objects.addAll(cso.getCompositeAsList());
			}
		if (requests != null)
			for (final CompositeRequestObject cro : requests) {
				this.objects.addAll(cro.getCompositeAsList());
			}

		if (svecList != null)
			this.svecList = svecList;
		else
			this.svecList = Collections.emptyList();

		if (requests == null || requests.isEmpty())
			throw new IllegalArgumentException("At least one CompositeRequestObject is mandatory.");
		this.requests = requests;

	}

	/**
	 * Gets list of {@link CompositeRequestSvecObject}.
	 * 
	 * @return List<CompositeSvecObject>. Can't be null, but may be empty.
	 */
	public List<CompositeRequestSvecObject> getSvecObjects() {
		return this.svecList;
	}

	/**
	 * Gets list of {@link CompositeRequestObject}.
	 * 
	 * @return List<CompositeRequestObject>. Can't be null or empty.
	 */
	public List<CompositeRequestObject> getRequests() {
		return this.requests;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.requests == null) ? 0 : this.requests.hashCode());
		result = prime * result + ((this.svecList == null) ? 0 : this.svecList.hashCode());
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
		final PCEPRequestMessage other = (PCEPRequestMessage) obj;
		if (this.requests == null) {
			if (other.requests != null)
				return false;
		} else if (!this.requests.equals(other.requests))
			return false;
		if (this.svecList == null) {
			if (other.svecList != null)
				return false;
		} else if (!this.svecList.equals(other.svecList))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPRequestMessage [svecObjs=");
		builder.append(this.svecList);
		builder.append(", requests=");
		builder.append(this.requests);
		builder.append("]");
		return builder.toString();
	}

}
