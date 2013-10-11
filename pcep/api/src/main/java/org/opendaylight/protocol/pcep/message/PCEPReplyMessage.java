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
import org.opendaylight.protocol.pcep.object.CompositeReplySvecObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure for Reply Message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.5">Reply Message</a>
 */
public class PCEPReplyMessage implements Message {

	private final List<CompositeReplySvecObject> svecList;

	private final List<CompositeResponseObject> responses;

	private final List<PCEPObject> objects;

	/**
	 * Constructs new Reply Message.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeResponseObject} in the list.
	 * 
	 * @param responses List<CompositeResponseObject>. Can't be empty or null.
	 */
	public PCEPReplyMessage(final List<CompositeResponseObject> responses) {
		this(responses, null);
	}

	/**
	 * Constructs {@link PCEPReplyMessage}.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeResponseObject} in the list.
	 * 
	 * @param svecList List<CompositeSvecObject>
	 * @param responses List<CompositeResponseObject>. Can't be empty or null.
	 */
	public PCEPReplyMessage(final List<CompositeResponseObject> responses, final List<CompositeReplySvecObject> svecList) {
		this.objects = Lists.newArrayList();
		if (svecList != null) {
			for (final CompositeReplySvecObject cno : svecList) {
				this.objects.addAll(cno.getCompositeAsList());
			}
		}
		if (responses != null) {
			for (final CompositeResponseObject cno : responses) {
				this.objects.addAll(cno.getCompositeAsList());
			}
		}

		if (responses == null || responses.isEmpty()) {
			throw new IllegalArgumentException("At least one CompositeResponseObject is mandatory.");
		}
		this.responses = responses;

		if (svecList != null) {
			this.svecList = svecList;
		} else {
			this.svecList = Collections.emptyList();
		}
	}

	/**
	 * Gets list of {@link CompositeResponseObject}.
	 * 
	 * @return List<CompositeResponseObject>. Can't be null or empty.
	 */
	public List<CompositeResponseObject> getResponses() {
		return this.responses;
	}

	/**
	 * Gets list of {@link CompositeReplySvecObject}.
	 * 
	 * @return List<CompositeReplySvecObject>. Can't be null but may be empty.
	 */
	public List<CompositeReplySvecObject> getSvecList() {
		return this.svecList;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.responses == null) ? 0 : this.responses.hashCode());
		result = prime * result + ((this.svecList == null) ? 0 : this.svecList.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final PCEPReplyMessage other = (PCEPReplyMessage) obj;
		if (this.responses == null) {
			if (other.responses != null) {
				return false;
			}
		} else if (!this.responses.equals(other.responses)) {
			return false;
		}
		if (this.svecList == null) {
			if (other.svecList != null) {
				return false;
			}
		} else if (!this.svecList.equals(other.svecList)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPReplyMessage [svecList=");
		builder.append(this.svecList);
		builder.append(", responses=");
		builder.append(this.responses);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public Class<Message> getImplementedInterface() {
		return Message.class;
	}
}
