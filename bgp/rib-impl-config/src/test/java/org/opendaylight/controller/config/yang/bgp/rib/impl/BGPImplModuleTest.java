/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
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
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;

public class BGPImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "bgp-impl1";

	private BGPImplModuleFactory factory;

	private BGPSessionProposalImplModuleFactory sessionFacotry;

	private BGPDispatcherImplModuleFactory dispactherFactory;

	private NettyThreadgroupModuleFactory threadgropFactory;

	private RIBExtensionsImplModuleFactory messageFactory;

	private SimpleBGPExtensionProviderContextModuleFactory extensionFactory;

	@Before
	public void setUp() throws Exception {
		this.factory = new BGPImplModuleFactory();
		this.dispactherFactory = new BGPDispatcherImplModuleFactory();
		this.sessionFacotry = new BGPSessionProposalImplModuleFactory();
		this.threadgropFactory = new NettyThreadgroupModuleFactory();
		this.messageFactory = new RIBExtensionsImplModuleFactory();
		this.extensionFactory = new SimpleBGPExtensionProviderContextModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.dispactherFactory, this.sessionFacotry, this.messageFactory, this.threadgropFactory, this.extensionFactory));
	}

	@Test
	public void testValidationExceptionPortNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, "localhost", null,
					this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
					this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
					this.extensionFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Port value is not set."));
		}
	}

	@Test
	public void testValidationExceptionPortOutOfRange() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, "localhost", -1,
					this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
					this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
					this.extensionFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("is out of range (0-65535)."));
		}
	}

	@Test
	public void testValidationExceptionHostNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, null, 1,
					this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
					this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
					this.extensionFactory.getImplementationName());
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Host value is not set."));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, "localhost", 1,
				this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
				this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
				this.extensionFactory.getImplementationName());
		transaction.validateConfig();
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 6, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, "localhost", 1,
				this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
				this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
				this.extensionFactory.getImplementationName());
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, this.factory.getImplementationName());
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 0, 0, 6);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, "localhost", 1,
				this.sessionFacotry.getImplementationName(), this.dispactherFactory.getImplementationName(),
				this.threadgropFactory.getImplementationName(), this.messageFactory.getImplementationName(),
				this.extensionFactory.getImplementationName());
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, this.factory.getImplementationName());
		final BGPImplModuleMXBean mxBean = transaction.newMBeanProxy(
				transaction.lookupConfigBean(this.factory.getImplementationName(), this.instanceName), BGPImplModuleMXBean.class);
		mxBean.setPort(10);
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 0, 1, 5);
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final String host, final Integer port, final String sessionModuleName,
			final String dispatcherModuleName, final String threadgroupModuleName, final String messageFactoryModuleName,
			final String extensionModuleName) throws InstanceAlreadyExistsException {
		final ObjectName nameCreated = transaction.createModule(moduleName, instanceName);
		final BGPImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, BGPImplModuleMXBean.class);
		mxBean.setHost(host);
		mxBean.setPort(port);
		mxBean.setBgpProposal(BGPSessionProposalImplModuleTest.createInstance(transaction, sessionModuleName, "bgp-session1", 1L,
				(short) 30, "128.0.0.1"));
		mxBean.setBgpDispatcher(BGPDispatcherImplModuleTest.createInstance(transaction, dispatcherModuleName, "bgp-dispatcher1"));
		return nameCreated;
	}

}
