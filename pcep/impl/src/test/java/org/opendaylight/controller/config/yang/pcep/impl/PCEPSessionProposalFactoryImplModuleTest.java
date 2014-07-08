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
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class PCEPSessionProposalFactoryImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "pcep-session-proposal-factory-impl";
    private static final String FACTORY_NAME = PCEPSessionProposalFactoryImplModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        final PCEPSessionProposalFactoryImplModuleFactory factory = new PCEPSessionProposalFactoryImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory));
    }

    @Test
    public void testValidationExceptionDeadTimerValueNotSet() throws Exception {
        try {
            createSessionInstance(null, (short) 100);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeadTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerNotSet() throws Exception {
        try {
            createSessionInstance((short) 200, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("KeepAliveTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerMinValue() throws Exception {
        try {
            createSessionInstance((short) 200, (short) -10);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("minimum value is 1."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME,
                INSTANCE_NAME), PCEPSessionProposalFactoryImplModuleMXBean.class);
        mxBean.setKeepAliveTimerValue((short) 180);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 0);
    }

    private CommitStatus createInstance() throws Exception {
        return createSessionInstance((short) 200, (short) 100);
    }

    private CommitStatus createSessionInstance(final Short deadTimer, final Short keepAlive) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createSessionInstance(transaction, deadTimer, keepAlive);
        return transaction.commit();
    }

    private static ObjectName createSessionInstance(final ConfigTransactionJMXClient transaction, final Short deadTimer,
            final Short keepAlive) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final PCEPSessionProposalFactoryImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
                PCEPSessionProposalFactoryImplModuleMXBean.class);
        mxBean.setDeadTimerValue(deadTimer);
        mxBean.setKeepAliveTimerValue(keepAlive);
        return nameCreated;
    }

    public static ObjectName createSessionInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        return createSessionInstance(transaction, (short) 180, (short) 30);
    }
}
