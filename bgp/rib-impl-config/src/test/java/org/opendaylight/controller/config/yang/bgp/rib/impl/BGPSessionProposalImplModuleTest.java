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

public class BGPSessionProposalImplModuleTest extends AbstractConfigTest {

	private final String instanceName = "bgp-session-prop";

	private BGPSessionProposalImplModuleFactory factory;

	@Before
	public void setUp() throws Exception {
		this.factory = new BGPSessionProposalImplModuleFactory();
		super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory));
	}

	@Test
	public void testValidationExceptionAsNumberNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, null, (short) 180, "192.168.1.1");
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("AsNumber value is not set."));
		}
	}

	@Test
	public void testValidationExceptionAsNumberMinValue() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, (long) 0, (short) 180, "192.168.1.1");
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("AsNumber value must be greater than 0"));
		}
	}

	@Test
	public void testValidationExceptionHoldtimerNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, null, "192.168.1.1");
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Holdtimer value is not set."));
		}
	}

	@Test
	public void testValidationExceptionHoldtimerMinValue() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 2, "192.168.1.1");
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("Holdtimer value must be"));
		}
	}

	@Test
	public void testValidationExceptionBgpIdNotSet() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 180, null);
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("BgpId value is not set."));
		}
	}

	@Test
	public void testValidationExceptionBgpIdNotIpv4() throws InstanceAlreadyExistsException {
		try {
			final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
			createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 180, "192.168.1.500");
			transaction.validateConfig();
			fail();
		} catch (final ValidationException e) {
			assertTrue(e.getMessage().contains("is not valid IPv4 address"));
		}
	}

	@Test
	public void testCreateBean() throws Exception {
		final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 180, "192.168.1.1");
		transaction.validateConfig();
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 1, 0, 0);
	}

	@Test
	public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
		ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 180, "192.168.1.1");
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
		createInstance(transaction, this.factory.getImplementationName(), this.instanceName, 1L, (short) 180, "192.168.1.1");
		transaction.commit();
		transaction = this.configRegistryClient.createTransaction();
		assertBeanCount(1, this.factory.getImplementationName());
		final BGPSessionProposalImplModuleMXBean mxBean = transaction.newMBeanProxy(
				transaction.lookupConfigBean(this.factory.getImplementationName(), this.instanceName),
				BGPSessionProposalImplModuleMXBean.class);
		mxBean.setBgpId("192.168.10.10");
		final CommitStatus status = transaction.commit();
		assertBeanCount(1, this.factory.getImplementationName());
		assertStatus(status, 0, 1, 0);
	}

	public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
			final String instanceName, final Long asNumber, final Short holdtimer, final String bgpId)
			throws InstanceAlreadyExistsException {
		final ObjectName nameCreated = transaction.createModule(moduleName, instanceName);
		final BGPSessionProposalImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, BGPSessionProposalImplModuleMXBean.class);
		mxBean.setAsNumber(asNumber);
		mxBean.setBgpId(bgpId);
		mxBean.setHoldtimer(holdtimer);
		return nameCreated;

	}
}
