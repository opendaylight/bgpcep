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
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.6">Notification
 *      (PCNtf) Message</a> - &lt;notify&gt;
 */
public class CompositeNotifyObject {

	private List<PCEPRequestParameterObject> requestParameters;

	private final List<PCEPNotificationObject> notifications;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 *
	 * @param notifications
	 *            List<PCEPNotificationObject>. Can't be null or empty.
	 */
	public CompositeNotifyObject(List<PCEPNotificationObject> notifications) {
		this(null, notifications);
	}

	/**
	 * Constructs composite object with optional objects.
	 *
	 * @param requestParameters
	 *            List<PCEPRequestParameterObject>
	 * @param notifications
	 *            List<PCEPNotificationObject>. Can't be null or empty.
	 */
	public CompositeNotifyObject(List<PCEPRequestParameterObject> requestParameters, List<PCEPNotificationObject> notifications) {
		if (notifications == null || notifications.isEmpty())
			throw new IllegalArgumentException("Notification Object is mandatory.");
		if (requestParameters != null)
			this.requestParameters = requestParameters;
		else
			this.requestParameters = Collections.emptyList();
		this.notifications = notifications;
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 *
	 * @return List<PCEPObject>
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		if (this.requestParameters != null && !this.requestParameters.isEmpty())
			list.addAll(this.requestParameters);
		list.addAll(this.notifications);
		return list;
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * @param objects List<PCEPObject> list of PCEPObjects from whose this
	 * object should be created.
	 * @return CompositeNotifyObject
	 */
	public static CompositeNotifyObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty())
			throw new IllegalArgumentException("List cannot be null or empty.");

		final List<PCEPRequestParameterObject> requestParameters = new ArrayList<PCEPRequestParameterObject>();
		final List<PCEPNotificationObject> notifications = new ArrayList<PCEPNotificationObject>();

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
					if (obj instanceof PCEPNotificationObject) {
						notifications.add((PCEPNotificationObject) obj);
						state = 2;
						break;
					}
			}

			if (state == 3) {
				break;
			}

			objects.remove(obj);
		}

		if (notifications.isEmpty())
			return null;

		return new CompositeNotifyObject(requestParameters, notifications);
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
	 * Gets list of {@link PCEPNotificationObject}.
	 *
	 * @return List<PCEPNotificationObject>. Can't be null or empty.
	 */
	public List<PCEPNotificationObject> getNotificationObjects() {
		return this.notifications;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.notifications == null) ? 0 : this.notifications.hashCode());
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
		final CompositeNotifyObject other = (CompositeNotifyObject) obj;
		if (this.notifications == null) {
			if (other.notifications != null)
				return false;
		} else if (!this.notifications.equals(other.notifications))
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
		builder.append("CompositeNotifyObject [requestParameters=");
		builder.append(this.requestParameters);
		builder.append(", notifications=");
		builder.append(this.notifications);
		builder.append("]");
		return builder.toString();
	}
}
