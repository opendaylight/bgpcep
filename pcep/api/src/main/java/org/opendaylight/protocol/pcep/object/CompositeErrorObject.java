/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Structure that combines set of related objects.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.7">Error (PCErr)
 *      Message</a> - &lt;error&gt;
 */
public class CompositeErrorObject {

	private List<PCEPRequestParameterObject> requestParameters;

	private final List<PCEPErrorObject> errors;

	public CompositeErrorObject(final PCEPRequestParameterObject requestParameter, final PCEPErrorObject error) {
		this(new ArrayList<PCEPRequestParameterObject>() {
			private static final long serialVersionUID = -3974192068960284132L;

			{
				if (requestParameter != null)
					this.add(requestParameter);
			}
		}, new ArrayList<PCEPErrorObject>() {
			private static final long serialVersionUID = -3976331879683713909L;

			{
				if (error != null)
					this.add(error);
			}
		});
	}

	/**
	 * Constructs basic composite object only with mandatory objects.
	 * 
	 * @param errors
	 *            List<PCEPErrorObject>. Can't be null or empty.
	 */
	public CompositeErrorObject(List<PCEPErrorObject> errors) {
		this(null, errors);
	}

	/**
	 * Constructs composite object with optional objects.
	 * 
	 * @param requestParameters
	 *            List<PCEPRequestParameterObject>
	 * @param errors
	 *            List<PCEPErrorObject>. Can't be null or empty.
	 */
	public CompositeErrorObject(List<PCEPRequestParameterObject> requestParameters, List<PCEPErrorObject> errors) {

		if (errors == null || errors.isEmpty())
			throw new IllegalArgumentException("Error Object is mandatory.");
		this.errors = errors;
		if (requestParameters != null)
			this.requestParameters = requestParameters;
		else
			this.requestParameters = Collections.emptyList();
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 * 
	 * @return List<PCEPObject>. Can't be null or empty.
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		if (this.requestParameters != null && !this.requestParameters.isEmpty())
			list.addAll(this.requestParameters);
		list.addAll(this.errors);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * 
	 * @param objects
	 *            List<PCEPObject> list of PCEPObjects from whose this object
	 *            should be created.
	 * @return CompositeErrorObject
	 */
	public static CompositeErrorObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty())
			throw new IllegalArgumentException("List cannot be null or empty.");

		final List<PCEPRequestParameterObject> requestParameters = new ArrayList<PCEPRequestParameterObject>();
		final List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();

		int state = 1;
		while (!objects.isEmpty()) {
			final PCEPObject obj = objects.get(0);
			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPRequestParameterObject) {
						requestParameters.add((PCEPRequestParameterObject) obj);
						state = 1;
						break;
					}
				case 2:
					state = 3;
					if (obj instanceof PCEPErrorObject) {
						errors.add((PCEPErrorObject) obj);
						state = 2;
						break;
					}
			}

			if (state == 3) {
				break;
			}

			objects.remove(obj);
		}

		if (errors.isEmpty())
			throw new IllegalArgumentException("Atleast one PCEPErrorObject is mandatory.");

		return new CompositeErrorObject(requestParameters, errors);
	}

	/**
	 * Gets list of {@link PCEPRequestParameterObject}.
	 * 
	 * @return List<PCEPRequestParameterObject>. Can't be null, but may be
	 *         empty.
	 */
	public List<PCEPRequestParameterObject> getRequestParameters() {
		return this.requestParameters;
	}

	/**
	 * Gets list of {@link PCEPErrorObject}
	 * 
	 * @return List<PCEPErrorObject>. Can't be null or empty.
	 */
	public List<PCEPErrorObject> getErrors() {
		return this.errors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.errors == null) ? 0 : this.errors.hashCode());
		result = prime * result + ((this.requestParameters == null) ? 0 : this.requestParameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final CompositeErrorObject other = (CompositeErrorObject) obj;
		if (this.errors == null) {
			if (other.errors != null)
				return false;
		} else if (!this.errors.equals(other.errors))
			return false;
		if (this.requestParameters == null) {
			if (other.requestParameters != null)
				return false;
		} else if (!this.requestParameters.equals(other.requestParameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeErrorObject [requestParameters=");
		builder.append(this.requestParameters);
		builder.append(", errors=");
		builder.append(this.errors);
		builder.append("]");
		return builder.toString();
	}
}
