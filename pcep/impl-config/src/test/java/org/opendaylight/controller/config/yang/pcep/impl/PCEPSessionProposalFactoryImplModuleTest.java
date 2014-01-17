/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.impl;

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

public class PCEPSessionProposalFactoryImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "pcep-proposal";

	private PCEPSessionProposalFactoryImplModuleFactory factory;

	@Before
	public void setUp() throws Exception {
		this.factory = new PCEPSessionProposalFactoryImplModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory));
	}

	@Test
	public void testValidationExceptionDeadTimerValueNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, null, 100, true, true, true, true);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("DeadTimerValue value is not set"));
		}
	}

	@Test
	public void testValidationExceptionKeepAliveTimerNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, null, true, true, true, true);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("KeepAliveTimerValue value is not set"));
		}
	}

	@Test
	public void testValidationExceptionStatefulNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, null, false, false, false);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Stateful value is not set"));
		}
	}

	@Test
	public void testValidationExceptionActiveNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, true, null, true, true);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Active value is not set"));
		}
	}

	@Test
	public void testValidationExceptionInstantiatedNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, true, true, true, null);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Initiated value is not set"));
		}
	}

	@Test
	public void testValidationExceptionKeepAliveTimerMinValue() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, -10, true, true, true, true);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("minimum value is 1."));
		}
	}

	@Test
	public void testStatefulAfterCommitted() throws InstanceAlreadyExistsException, InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, false, true, true, true);
		transaction.validateConfig();
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		final PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMBeanProxy(
				transaction.lookupConfigBean(AbstractPCEPSessionProposalFactoryImplModuleFactory.NAME, this.instanceName),
				PCEPSessionProposalFactoryImplModuleMXBean.class);
		assertTrue(mxBean.getStateful());
	}

	@Test
	public void testCreateBean() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 0, 0, true, true, true, true);
		transaction.validateConfig();
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 1, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, true, true, true, true);
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, this.factory.getImplementationName());
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 0, 0, 1);
	}

	@Test
	public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException,
			InstanceNotFoundException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 400, 100, true, true, true, true);
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, this.factory.getImplementationName());
		final PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMBeanProxy(
				transaction.lookupConfigBean(AbstractPCEPSessionProposalFactoryImplModuleFactory.NAME, this.instanceName),
				PCEPSessionProposalFactoryImplModuleMXBean.class);
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 0, 0, 1);
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final Integer deadTimer, final Integer keepAlive, final Boolean stateful, final Boolean active,
			final Boolean versioned, final Boolean instant) throws InstanceAlreadyExistsException {
		final ObjectName nameCreated = transaction.createModule(moduleName, instanceName);
		final PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
				PCEPSessionProposalFactoryImplModuleMXBean.class);
		mxBean.setActive(active);
		mxBean.setDeadTimerValue(deadTimer);
		mxBean.setInitiated(instant);
		mxBean.setKeepAliveTimerValue(keepAlive);
		mxBean.setStateful(stateful);
		mxBean.setVersioned(versioned);
		return nameCreated;
	}

}
