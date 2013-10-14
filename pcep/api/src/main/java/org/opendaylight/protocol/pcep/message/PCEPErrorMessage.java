/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure of Error Message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.7">Error Message</a>
 */
public class PCEPErrorMessage implements Message {

	private PCEPOpenObject openObj;

	private final List<PCEPErrorObject> errorObjects;

	private final List<CompositeErrorObject> errors;

	private final List<PCEPObject> objects;

	public PCEPErrorMessage(final PCEPErrorObject errorObject) {
		this(new ArrayList<PCEPErrorObject>() {
			private static final long serialVersionUID = 72172137965955228L;

			{
				this.add(errorObject);
			}
		});
	}

	public PCEPErrorMessage(final CompositeErrorObject compositeErrorObject) {
		this(new ArrayList<CompositeErrorObject>() {
			private static final long serialVersionUID = 72172137965955228L;

			{
				if (compositeErrorObject != null) {
					this.add(compositeErrorObject);
				}
			}
		});
	}

	/**
	 * Constructs Error Message from list of {@link PCEPErrorObject} or {@link CompositeErrorObject}.
	 * 
	 * @param errorObjects List<?> either objects of type: {@link PCEPErrorObject} or {@link CompositeErrorObject}
	 * 
	 * @throws IllegalArgumentException if any other type is passed in the list, that cannot be processed
	 */
	public PCEPErrorMessage(final List<?> errorObjects) {
		this.objects = Lists.newArrayList();
		if (errorObjects != null) {
			for (int i = 0; i < errorObjects.size(); i++) {
				if (errorObjects.get(i) instanceof CompositeErrorObject) {
					this.objects.addAll(((CompositeErrorObject) errorObjects.get(i)).getCompositeAsList());
				} else if (errorObjects.get(i) instanceof PCEPErrorObject) {
					this.objects.add((PCEPErrorObject) errorObjects.get(i));
				}
			}
		}
		this.errors = new ArrayList<CompositeErrorObject>();
		this.errorObjects = new ArrayList<PCEPErrorObject>();

		if (errorObjects != null) {
			for (int i = 0; i < errorObjects.size(); i++) {
				if (errorObjects.get(i) instanceof CompositeErrorObject) {
					this.errors.add((CompositeErrorObject) errorObjects.get(i));
				} else if (errorObjects.get(i) instanceof PCEPErrorObject) {
					this.errorObjects.add((PCEPErrorObject) errorObjects.get(i));
				} else {
					throw new IllegalArgumentException("Wrong instance passed in list. Acceptable is only CompositeErrorObject or PCEPErrorObject.");
				}
			}
		}
	}

	/**
	 * Constructs Error Message from list of {@link PCEPErrorObject} and {@link CompositeErrorObject} and
	 * {@link PCEPOpenObject} that cannot be null. This constructor is used during PCEP handshake to suggest new session
	 * characteristics for the session that are listen in {@link PCEPOpenObject}.
	 * 
	 * @param openObj {@link PCEPOpenObject} cannot be null
	 * @param errorObjects List<PCEPErrorObject> list of error objects
	 * @param errors List<CompositeErrorObject> list of composite error objects
	 */
	public PCEPErrorMessage(final PCEPOpenObject openObj, final List<PCEPErrorObject> errorObjects, final List<CompositeErrorObject> errors) {
		this.objects = Lists.newArrayList();
		if (errorObjects != null) {
			this.objects.addAll(errorObjects);
		}
		if (openObj != null) {
			this.objects.add(openObj);
		}
		if (errors != null) {
			for (final CompositeErrorObject ceo : errors) {
				this.objects.addAll(ceo.getCompositeAsList());
			}
		}

		this.openObj = openObj;

		if (errorObjects == null) {
			throw new IllegalArgumentException("At least one PCEPErrorObject is mandatory.");
		}
		this.errorObjects = errorObjects;

		if (errors == null) {
			this.errors = Collections.emptyList();
		} else {
			this.errors = errors;
		}
	}

	/**
	 * Gets {@link PCEPOpenObject} if this is included. If its included, it proposes alternative acceptable session
	 * characteristic values.
	 * 
	 * @return PCEPOpenObject. May be null.
	 */
	public PCEPOpenObject getOpenObject() {
		return this.openObj;
	}

	/**
	 * In unsolicited manner can be included List of <code>PCEPErrorObjects</code> <code>PCEPErrorMessage</code>, which
	 * is not sent in response to a request.
	 * 
	 * @return List<PCEPErrorObject>
	 */
	public List<PCEPErrorObject> getErrorObjects() {
		return this.errorObjects;
	}

	/**
	 * If the PCErr message is sent in response to a request, the PCErr message MUST include set of RP objects related
	 * to pending path computation requests that triggered the error condition. In this situation it is constructed as
	 * {@link org.opendaylight.protocol.pcep.object.CompositeErrorObject CompCompositeErrorObject}. That includes list
	 * of RP objects.
	 * 
	 * @return CompositeErrorObject. May be null.
	 */
	public List<CompositeErrorObject> getErrors() {
		return this.errors;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.errorObjects == null) ? 0 : this.errorObjects.hashCode());
		result = prime * result + ((this.errors == null) ? 0 : this.errors.hashCode());
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
		final PCEPErrorMessage other = (PCEPErrorMessage) obj;
		if (this.errorObjects == null) {
			if (other.errorObjects != null) {
				return false;
			}
		} else if (!this.errorObjects.equals(other.errorObjects)) {
			return false;
		}
		if (this.errors == null) {
			if (other.errors != null) {
				return false;
			}
		} else if (!this.errors.equals(other.errors)) {
			return false;
		}
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
		builder.append("PCEPErrorMessage [openObj=");
		builder.append(this.openObj);
		builder.append(", errorObjects=");
		builder.append(this.errorObjects);
		builder.append(", errors=");
		builder.append(this.errors);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public Class<Message> getImplementedInterface() {
		return Message.class;
	}
}
