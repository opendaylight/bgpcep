/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.osgi;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

import javax.management.MBeanServer;

import org.opendaylight.controller.config.persist.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.config.persist.NoOpStorageAdapter;
import org.opendaylight.controller.config.persist.PersisterImpl;
import org.opendaylight.netconf.util.osgi.ConfigProvider;
import org.opendaylight.netconf.util.osgi.ConfigProvider.ConfigProviderImpl;
import org.opendaylight.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.netconf.util.osgi.NetconfConfigUtil.TLSConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class ConfigPersisterActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterActivator.class);

	private final static MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

	private ConfigPersisterNotificationHandler configPersisterNotificationHandler;

	private Thread initializationThread;

	@Override
	public void start(BundleContext context) throws Exception {
		logger.debug("ConfigPersister activator started");

		ConfigProvider configProvider = new ConfigProviderImpl(context);
		Optional<PersisterImpl> maybePersister = PersisterImpl.createFromProperties(configProvider);
		if (maybePersister.isPresent() == false) {
			throw new IllegalStateException("No persister is defined in " + PersisterImpl.STORAGE_ADAPTER_CLASS_PROP
					+ " property. For noop persister use " + NoOpStorageAdapter.class.getCanonicalName()
					+ " . Persister is not operational");
		}

		Optional<TLSConfiguration> maybeTLSConfiguration = NetconfConfigUtil.extractTLSConfiguration(configProvider);
		Optional<InetSocketAddress> maybeTCPAddress = NetconfConfigUtil.extractTCPNetconfAddress(configProvider);

		InetSocketAddress address;
		if (maybeTLSConfiguration.isPresent()) {
			throw new UnsupportedOperationException("TLS is currently not supported");
		} else if (maybeTCPAddress.isPresent()) {
			address = maybeTCPAddress.get();
		} else {
			throw new IllegalStateException("Netconf is not configured, persister is not operational");
		}

		PersisterImpl persister = maybePersister.get();
		configPersisterNotificationHandler = new ConfigPersisterNotificationHandler(persister, address, platformMBeanServer);
		Runnable initializationRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					configPersisterNotificationHandler.init();
				} catch (InterruptedException e) {
					logger.info("Interrupted while waiting for netconf connection");
				}
			}
		};
		initializationThread = new Thread(initializationRunnable, "ConfigPersister-registrator");
		initializationThread.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		initializationThread.interrupt();
		configPersisterNotificationHandler.close();
	}
}
