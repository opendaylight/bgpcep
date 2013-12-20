/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;

public class BGPDispatcherImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "bgp-message-fct";

	private BGPDispatcherImplModuleFactory factory;

	private NettyThreadgroupModuleFactory threadgroupFactory;

	private RIBExtensionsImplModuleFactory messageFactory;

	private SimpleBGPExtensionProviderContextModuleFactory extensionFactory;

	@Before
	public void setUp() throws Exception {
		this.factory = new BGPDispatcherImplModuleFactory();
		this.threadgroupFactory = new NettyThreadgroupModuleFactory();
		this.messageFactory = new RIBExtensionsImplModuleFactory();
		this.extensionFactory = new SimpleBGPExtensionProviderContextModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, threadgroupFactory, messageFactory, extensionFactory));
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 4, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
	ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 4);
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
                                            final String instanceName) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		BGPDispatcherImplModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, BGPDispatcherImplModuleMXBean.class);
		mxBean.setBossGroup(createThreadgroupInstance(transaction, "boss-threadgroup", 10));
		mxBean.setWorkerGroup(createThreadgroupInstance(transaction, "worker-threadgroup", 10));
		mxBean.setBgpExtensions(createBgpExtensionsInstance(transaction, "bgp-extensions"));
		return nameCreated;
	}

	public static ObjectName createThreadgroupInstance(
			final ConfigTransactionJMXClient transaction,
			final String instanceName,
			final Integer threadCount) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(NettyThreadgroupModuleFactory.NAME,
				instanceName);
		NettyThreadgroupModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, NettyThreadgroupModuleMXBean.class);
		mxBean.setThreadCount(threadCount);
		return nameCreated;
	}

	public static ObjectName createBgpExtensionsInstance(
			final ConfigTransactionJMXClient transaction,
			final String instanceName)
			throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(SimpleBGPExtensionProviderContextModuleFactory.NAME,
				instanceName);
		transaction.newMBeanProxy(nameCreated,
				SimpleBGPExtensionProviderContextModuleMXBean.class);
		return nameCreated;
	}
}
