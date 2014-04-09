/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.impl;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PCEPSessionProposalFactoryImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "pcep-session-proposal-factory-impl";
    private static final String FACTORY_NAME = PCEPSessionProposalFactoryImplModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        final PCEPSessionProposalFactoryImplModuleFactory factory = new PCEPSessionProposalFactoryImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory));
    }

    @Test
    public void testValidationExceptionDeadTimerValueNotSet() throws InstanceAlreadyExistsException,
            ConflictingVersionException {
        try {
            createSessionInstance(null, 100);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeadTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerNotSet() throws InstanceAlreadyExistsException,
            ConflictingVersionException {
        try {
            createSessionInstance(400, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("KeepAliveTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerMinValue() throws InstanceAlreadyExistsException,
            ConflictingVersionException {
        try {
            createSessionInstance(400, -10);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("minimum value is 1."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createSessionInstance(0, 0);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createSessionInstance(400, 100);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws Exception {
        createSessionInstance(400, 100);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        transaction.newMBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                PCEPSessionProposalFactoryImplModuleMXBean.class);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    private CommitStatus createSessionInstance(final Integer deadTimer, final Integer keepAlive)
            throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createSessionInstance(transaction, deadTimer, keepAlive);
        return transaction.commit();
    }

    public static ObjectName createSessionInstance(final ConfigTransactionJMXClient transaction,
            final Integer deadTimer, final Integer keepAlive) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                PCEPSessionProposalFactoryImplModuleMXBean.class);
        mxBean.setDeadTimerValue(deadTimer);
        mxBean.setKeepAliveTimerValue(keepAlive);
        return nameCreated;
    }
}
