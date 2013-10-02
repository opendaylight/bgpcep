/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import java.util.Set;

import javax.management.*;

import org.opendaylight.netconf.api.mapping.jmx.CommitJMXNotification;
import org.opendaylight.netconf.api.mapping.jmx.DefaultCommitOperationMXBean;
import org.opendaylight.netconf.api.mapping.jmx.NetconfJMXNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class DefaultCommitNotificationProducer extends NotificationBroadcasterSupport implements DefaultCommitOperationMXBean,
		AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(DefaultCommitNotificationProducer.class);

	private final MBeanServer mbeanServer;

	private final ObjectName on = DefaultCommitOperationMXBean.objectName;

	public DefaultCommitNotificationProducer(MBeanServer mBeanServer) {
		this.mbeanServer = mBeanServer;
		registerMBean(this, mbeanServer, on);
	}

	private static void registerMBean(final Object instance, final MBeanServer mbs, final ObjectName on) {
		try {
			mbs.registerMBean(instance, on);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
			throw new RuntimeException("Unable to register " + instance + " as " + on, e);
		}
	}

	public void sendCommitNotification(String message, Element cfgSnapshot, Set<String> capabilities) {
		CommitJMXNotification notif = NetconfJMXNotification.afterCommit(this, message, cfgSnapshot, capabilities);
		logger.debug("Notification about commit {} sent", notif);
		sendNotification(notif);
	}

	@Override
	public void close() {
		try {
			mbeanServer.unregisterMBean(on);
		} catch (InstanceNotFoundException | MBeanRegistrationException e) {
			logger.warn("Ignoring exception while unregistering {} as {}", this, on, e);
		}
	}
}
