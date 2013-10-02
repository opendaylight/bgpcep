/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.impl.osgi;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.opendaylight.netconf.impl.*;
import org.opendaylight.netconf.util.osgi.ConfigProvider;
import org.opendaylight.netconf.util.osgi.ConfigProvider.ConfigProviderImpl;
import org.opendaylight.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.netconf.util.osgi.NetconfConfigUtil.TLSConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import io.netty.util.HashedWheelTimer;

public class NetconfImplActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(NetconfImplActivator.class);

	private Optional<InetSocketAddress> maybeTCPAddress;
	private Optional<TLSConfiguration> maybeTLSConfiguration;

	private NetconfOperationServiceFactoryTracker factoriesTracker;
	private DefaultCommitNotificationProducer commitNot;
	private NetconfServerDispatcher dispatch;

	@Override
	public void start(final BundleContext context) throws Exception {
		final ConfigProvider configProvider = new ConfigProviderImpl(context);
		maybeTCPAddress = NetconfConfigUtil.extractTCPNetconfAddress(configProvider);
		maybeTLSConfiguration = NetconfConfigUtil.extractTLSConfiguration(configProvider);
		if (maybeTCPAddress.isPresent() == false && maybeTLSConfiguration.isPresent() == false) {
			throw new IllegalStateException("TCP nor TLS is configured, netconf not available.");
		}
		NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
		factoriesTracker = new NetconfOperationServiceFactoryTracker(context, factoriesListener);
		factoriesTracker.open();

		SessionIdProvider idProvider = new SessionIdProvider();
		NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(new HashedWheelTimer(), factoriesListener, idProvider);

		commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

		NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(factoriesListener, commitNot, idProvider);

		if (maybeTCPAddress.isPresent()) {
			Optional<SSLContext> maybeSSLContext = Optional.absent();
			InetSocketAddress address = maybeTCPAddress.get();
			dispatch = new NetconfServerDispatcher(maybeSSLContext, serverNegotiatorFactory, listenerFactory);

			logger.debug("Starting TCP netconf server at {}", address);
			dispatch.createServer(address);
		}
		if (maybeTLSConfiguration.isPresent()) {
			throw new UnsupportedOperationException("TLS not implemented");
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		logger.info("Shutting down netconf because YangStoreService service was removed");

		commitNot.close();
		dispatch.close();
	}
}
