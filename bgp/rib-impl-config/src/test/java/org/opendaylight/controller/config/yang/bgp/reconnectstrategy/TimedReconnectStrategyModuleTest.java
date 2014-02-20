/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.reconnectstrategy;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.AbstractTimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleFactory;
import org.opendaylight.controller.config.yang.reconnectstrategy.TimedReconnectStrategyModuleMXBean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimedReconnectStrategyModuleTest extends AbstractConfigTest {

	private final String instanceName = "timed";

	private TimedReconnectStrategyModuleFactory factory;

	private GlobalEventExecutorModuleFactory executorFactory;

	@Before
	public void setUp() throws Exception {
		this.factory = new TimedReconnectStrategyModuleFactory();
		this.executorFactory = new GlobalEventExecutorModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
				factory, executorFactory));
	}

	@Test
	public void testValidationExceptionSleepFactorNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, 500, 100L, null, 500L,
					10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("SleepFactor value is not set."));
		}
	}

	@Test
	public void testValidationExceptionSleepFactorMinValue()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, 500, 100L,
					new BigDecimal(0.5), 500L, 10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("is less than 1"));
		}
	}

	@Test
	public void testValidationExceptionConnectTimeNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, null, 100L,
					new BigDecimal(1.0), 500L, 10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("ConnectTime value is not set."));
		}
	}

	@Test
	public void testValidationExceptionConnectTimeMinValue()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, -1, 100L, new BigDecimal(
					1.0), 500L, 10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("is less than 0"));
		}
	}

	@Test
	public void testValidationExceptionMinSleepNotSet()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, 100, null,
					new BigDecimal(1.0), 100L, 10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains("MinSleep value is not set."));
		}
	}

	@Test
	public void testValidationExceptionMaxSleep()
			throws InstanceAlreadyExistsException {
		try {
			ConfigTransactionJMXClient transaction = configRegistryClient
					.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), instanceName, 100, 300L,
					new BigDecimal(1.0), 100L, 10L, 10000L);
			transaction.validateConfig();
			fail();
		} catch (ValidationException e) {
			assertTrue(e.getMessage().contains(
					"is greter than MaxSleep"));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, 500, 100L,
				new BigDecimal(1.0), 500L, 10L, 10000L);
		transaction.validateConfig();
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 2, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, 500, 100L,
				new BigDecimal(1.0), 500L, 10L, 10000L);
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 0, 2);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException,
			ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = configRegistryClient
				.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), instanceName, 500, 100L,
				new BigDecimal(1.0), new Long(500), new Long(10), new Long(
						10000));
		transaction.commit();
		transaction = configRegistryClient.createTransaction();
		assertBeanCount(1, factory.getImplementationName());
		TimedReconnectStrategyModuleMXBean mxBean = transaction
				.newMBeanProxy(transaction.lookupConfigBean(
						AbstractTimedReconnectStrategyModuleFactory.NAME,
						instanceName), TimedReconnectStrategyModuleMXBean.class);
		assertEquals(mxBean.getMinSleep(), new Long(100));
		mxBean.setMinSleep(200L);
		assertEquals(mxBean.getMinSleep(), new Long(200));
		CommitStatus status = transaction.commit();
		assertBeanCount(1, factory.getImplementationName());
		assertStatus(status, 0, 1, 1);

	}

	public static ObjectName createInstance(
			final ConfigTransactionJMXClient transaction,
			final String moduleName,
			final String instanceName, final Integer connectTime,
			final Long minSleep, final BigDecimal sleepFactor,
			final Long maxSleep, final Long maxAttempts, final Long deadline) throws InstanceAlreadyExistsException {
		ObjectName nameCreated = transaction.createModule(
				moduleName, instanceName);
		TimedReconnectStrategyModuleMXBean mxBean = transaction.newMBeanProxy(
				nameCreated, TimedReconnectStrategyModuleMXBean.class);
		mxBean.setConnectTime(connectTime);
		mxBean.setDeadline(deadline);
		mxBean.setMaxAttempts(maxAttempts);
		mxBean.setMaxSleep(maxSleep);
		mxBean.setMinSleep(minSleep);
		mxBean.setSleepFactor(sleepFactor);
		mxBean.setExecutor(GlobalEventExecutorUtil.createOrGetInstance(transaction));
		return nameCreated;
	}

}
