/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import javax.xml.parsers.ParserConfigurationException;

import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.mapping.jmx.CommitJMXNotification;
import org.opendaylight.netconf.api.mapping.jmx.DefaultCommitOperationMXBean;
import org.opendaylight.netconf.api.mapping.jmx.NetconfJMXNotification;
import org.opendaylight.netconf.client.NetconfClient;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.opendaylight.netconf.util.xml.Xml;
import org.opendaylight.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * Responsible for listening for notifications from netconf containing latest committed configuration that should be
 * persisted, and also for loading last configuration.
 */
@ThreadSafe
public class ConfigPersisterNotificationHandler implements NotificationListener, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterNotificationHandler.class);

	private final InetSocketAddress address;

	private NetconfClient netconfClient;

	private final PersisterImpl persister;
	private final MBeanServerConnection mbeanServer;
	private Long currentSessionId;

	private final ObjectName on = DefaultCommitOperationMXBean.objectName;

	public static final long DEFAULT_TIMEOUT = 40000L;
	private final long timeout;

	public ConfigPersisterNotificationHandler(PersisterImpl persister, InetSocketAddress address, MBeanServerConnection mbeanServer) {
		this(persister, address, mbeanServer, DEFAULT_TIMEOUT);
	}

	public ConfigPersisterNotificationHandler(PersisterImpl persister, InetSocketAddress address, MBeanServerConnection mbeanServer,
			long timeout) {
		this.persister = persister;
		this.address = address;
		this.mbeanServer = mbeanServer;
		this.timeout = timeout;
	}

	public void init() throws InterruptedException {
		Optional<Persister.PersistedConfig> maybeConfig = loadLastConfig();

		if (maybeConfig.isPresent()) {
			logger.debug("Last config found {}", persister);

			registerToNetconf(maybeConfig.get().getCapabilities());

			pushLastConfig(maybeConfig.get().getConfigSnapshot());

		} else {
			// this ensures that netconf is initialized, this is first connection
			// this means we can register as listener for commit
			registerToNetconf(Collections.<String> emptySet());

			logger.info("No last config provided by backend storage {}", persister);
		}
		registerAsJMXListener();
	}

	// TODO test init with new netconf client

	private class NamingThreadPoolFactory implements ThreadFactory {

		private final ThreadGroup group;
		private final AtomicLong threadName = new AtomicLong();

		public NamingThreadPoolFactory(String namePrefix) {
			Preconditions.checkNotNull(namePrefix);
			this.group = new ThreadGroup(namePrefix);
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(group, r, String.format("%s-%d", group.getName(), threadName.incrementAndGet()));
		}
	}

	private synchronized long registerToNetconf(Set<String> expectedCaps) throws InterruptedException {

		Set<String> currentCapabilities = Sets.newHashSet();

		// TODO think about moving capability subset check to netconf client
		// could be utilized by integration tests

		long pollingStart = System.currentTimeMillis();
		int delay = 1000;

		int attempt = 0;

		while (true) {
			attempt++;
			try {

				try {
					netconfClient = new NetconfClient(this.toString(), address, 3, delay);
					// TODO is this correct ex to catch ?
				} catch (IllegalStateException e) {
					logger.debug("Netconf was not initialized or is not stable, attempt {}", attempt, e);
				}
				currentCapabilities = netconfClient.getCapabilities();

				if (isSubset(currentCapabilities, expectedCaps)) {
					logger.debug("Hello from netconf stable with {} capabilities", currentCapabilities);
					currentSessionId = netconfClient.getSessionId();
					logger.info("Session id received from netconf server: {}", currentSessionId);
					return currentSessionId;
				}

				if (System.currentTimeMillis() > pollingStart + timeout) {
					break;
				}

				logger.debug("Polling hello from netconf, attempt {}, capabilities {}", attempt, currentCapabilities);

				try {
					netconfClient.close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing temporary client " + netconfClient);
				}

			} catch (RuntimeException e) {
				logger.debug("Netconf was not initialized or is not stable, attempt {}", attempt, e);
			}
		}

		throw new RuntimeException("Netconf server did not provide required capabilities " + expectedCaps
				+ " in time, provided capabilities " + currentCapabilities);

	}

	private boolean isSubset(Set<String> currentCapabilities, Set<String> expectedCaps) {
		for (String exCap : expectedCaps) {
			if (currentCapabilities.contains(exCap) == false)
				return false;
		}
		return true;
	}

	private void registerAsJMXListener() {
		try {
			mbeanServer.addNotificationListener(on, this, null, null);
		} catch (InstanceNotFoundException | IOException e) {
			throw new RuntimeException("Cannot register as JMX listener to netconf", e);
		}
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		if (notification instanceof NetconfJMXNotification == false)
			return;

		// Socket should not be closed at this point
		// Activator unregisters this as JMX listener before close is called

		logger.debug("Received notification {}", notification);
		if (notification instanceof CommitJMXNotification) {
			try {
				handleAfterCommitNotification((CommitJMXNotification) notification);
			} catch (Exception e) {
				// TODO: notificationBroadcast support logs only DEBUG
				logger.warn("Exception occured during notification handling: ", e);
				throw e;
			}
		} else
			throw new IllegalStateException("Unknown config registry notification type " + notification);
	}

	private void handleAfterCommitNotification(CommitJMXNotification notification) {
		try {
			XmlElement configElement = XmlElement.fromDomElement(notification.getConfigSnapshot());
			persister.persistConfig(configElement.getDomElement(), notification.getCapabilities());
			logger.debug("Configuration persisted successfully");
		} catch (IOException e) {
			throw new RuntimeException("Unable to persist configuration snapshot", e);
		}
	}

	private Optional<Persister.PersistedConfig> loadLastConfig() {
		Optional<Persister.PersistedConfig> maybeConfigElement;
		try {
			maybeConfigElement = persister.loadLastConfig();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load configuration", e);
		}
		return maybeConfigElement;
	}

	private synchronized void pushLastConfig(Element persistedConfig) {
		StringBuilder response = new StringBuilder("editConfig response = {");

		Element configElement = persistedConfig;
		NetconfMessage message = createEditConfigMessage(configElement, "/netconfOp/editConfig.xml");
		NetconfMessage responseMessage = netconfClient.sendMessage(message);

		XmlElement element = XmlElement.fromDomDocument(responseMessage.getDocument());
		Preconditions.checkState(element.getName().equals(Xml.RPC_REPLY_KEY));
		element = element.getOnlyChildElement();

		checkIsOk(element, responseMessage);
		response.append(Xml.toString(responseMessage.getDocument()));
		response.append("}");
		responseMessage = netconfClient.sendMessage(getNetconfMessageFromResource("/netconfOp/commit.xml"));

		element = XmlElement.fromDomDocument(responseMessage.getDocument());
		Preconditions.checkState(element.getName().equals(Xml.RPC_REPLY_KEY));
		element = element.getOnlyChildElement();

		checkIsOk(element, responseMessage);
		response.append("commit response = {");
		response.append(Xml.toString(responseMessage.getDocument()));
		response.append("}");
		logger.debug("Last configuration loaded successfully");
	}

	private void checkIsOk(XmlElement element, NetconfMessage responseMessage) {
		if (element.getName().equals(Xml.OK)) {
			return;
		} else {
			if (element.getName().equals(Xml.RPC_ERROR)) {
				logger.warn("Can not load last configuration, operation failed");
				throw new IllegalStateException("Can not load last configuration, operation failed: "
						+ Xml.toString(responseMessage.getDocument()));
			}
			logger.warn("Can not load last configuration. Operation failed.");
			throw new IllegalStateException("Can not load last configuration. Operation failed: "
					+ Xml.toString(responseMessage.getDocument()));
		}
	}

	private NetconfMessage createEditConfigMessage(Element dataElement, String editConfigResourcename) {
		try (InputStream stream = getClass().getResourceAsStream(editConfigResourcename)) {
			Preconditions.checkNotNull(stream, "Unable to load resource " + editConfigResourcename);

			Document doc = Xml.readXmlToDocument(stream);

			doc.getDocumentElement();
			XmlElement editConfigElement = XmlElement.fromDomDocument(doc).getOnlyChildElement();
			XmlElement configWrapper = editConfigElement.getOnlyChildElement(Xml.CONFIG_KEY);
			editConfigElement.getDomElement().removeChild(configWrapper.getDomElement());
			for (XmlElement el : XmlElement.fromDomElement(dataElement).getChildElements()) {
				configWrapper.appendChild((Element) doc.importNode(el.getDomElement(), true));
			}
			editConfigElement.appendChild(configWrapper.getDomElement());
			return new NetconfMessage(doc);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new RuntimeException("Unable to parse message from resources " + editConfigResourcename, e);
		}
	}

	private NetconfMessage getNetconfMessageFromResource(String resource) {
		try (InputStream stream = getClass().getResourceAsStream(resource)) {
			Preconditions.checkNotNull(stream, "Unable to load resource " + resource);
			return new NetconfMessage(XMLUtil.parse(stream));
		} catch (SAXException | IOException e) {
			throw new RuntimeException("Unable to parse message from resources " + resource, e);
		}
	}

	@Override
	public synchronized void close() {
		// TODO persister is received from constructor, should not be closed here
		try {
			persister.close();
		} catch (Exception e) {
			logger.warn("Unable to close config persister {}", persister, e);
		}

		if (netconfClient != null) {
			try {
				netconfClient.close();
			} catch (Exception e) {
				logger.warn("Unable to close connection to netconf {}", netconfClient, e);
			}
		}

		// unregister from JMX
		try {
			if (mbeanServer.isRegistered(on)) {
				mbeanServer.removeNotificationListener(on, this);
			}
		} catch (Exception e) {
			logger.warn("Unable to unregister {} as listener for {}", this, on, e);
		}
	}
}
