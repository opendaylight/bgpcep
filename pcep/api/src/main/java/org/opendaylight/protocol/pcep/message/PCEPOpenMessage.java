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
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure of Open Message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.2">Open Message</a>
 */
public class PCEPOpenMessage implements Message {

	private final PCEPOpenObject openObj;

	private final List<PCEPObject> objects;

	/**
	 * Constructs new Open Message.
	 * 
	 * @throws IllegalArgumentException if the PCEPOpenObject is null.
	 * 
	 * @param openObj {@link PCEPOpenObject}. Can't be null.
	 */
	public PCEPOpenMessage(final PCEPOpenObject openObj) {
		this.objects = Lists.newArrayList();
		if (openObj != null) {
			this.objects.add(openObj);
		} else {
			throw new IllegalArgumentException("PCEPOpenObject is mandatory.");
		}

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

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.openObj == null) ? 0 : this.openObj.hashCode());
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
		final PCEPOpenMessage other = (PCEPOpenMessage) obj;
		if (this.openObj == null) {
			if (other.openObj != null) {
				return false;
			}
		} else if (!this.openObj.equals(other.openObj)) {
			return false;
		}
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

	@Override
	public Class<Message> getImplementedInterface() {
		return Message.class;
	}
}
