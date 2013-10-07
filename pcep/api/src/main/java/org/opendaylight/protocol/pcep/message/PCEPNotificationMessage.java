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
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.collect.Lists;

/**
 * Structure of Notification Message.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-6.6">Notification Message</a>
 */
public class PCEPNotificationMessage implements Message {

	private final List<CompositeNotifyObject> notifications;

	private final List<PCEPObject> objects;

	/**
	 * Constructs new Notification Message.
	 * 
	 * @throws IllegalArgumentException if there is not even one {@link CompositeNotifyObject} in the list.
	 * 
	 * @param notifications List<CompositeNotifyObject>. Can't be empty or null.
	 */
	public PCEPNotificationMessage(final List<CompositeNotifyObject> notifications) {
		this.objects = Lists.newArrayList();
		if (notifications != null) {
			for (final CompositeNotifyObject cno : notifications) {
				this.objects.addAll(cno.getCompositeAsList());
			}
		}
		if (notifications == null || notifications.isEmpty())
			throw new IllegalArgumentException("At least one CompositeNotifyObject is mandatory.");

		this.notifications = notifications;
	}

	/**
	 * Gets list of {@link org.opendaylight.protocol.pcep.object.CompositeNotifyObject CompositeNotifyObjects}.
	 * 
	 * @return List<CompositeNotifyObject>. Can't be null or empty.
	 */
	public List<CompositeNotifyObject> getNotifications() {
		return this.notifications;
	}

	public List<PCEPObject> getAllObjects() {
		return this.objects;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.notifications == null) ? 0 : this.notifications.hashCode());
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
		final PCEPNotificationMessage other = (PCEPNotificationMessage) obj;
		if (this.notifications == null) {
			if (other.notifications != null)
				return false;
		} else if (!this.notifications.equals(other.notifications))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPNotificationMessage [notifications=");
		builder.append(this.notifications);
		builder.append("]");
		return builder.toString();
	}
}
