/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

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
import org.opendaylight.controller.config.yang.pcep.stateful02.cfg.Stateful02PCEPSessionProposalFactoryModuleFactory;
import org.opendaylight.controller.config.yang.pcep.stateful02.cfg.Stateful02PCEPSessionProposalFactoryModuleMXBean;

public class Stateful02PCEPSessionProposalFactoryModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "pcep-stateful02-session-proposal-factroy-impl";
    private static final String FACTROY_NAME = Stateful02PCEPSessionProposalFactoryModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, new Stateful02PCEPSessionProposalFactoryModuleFactory()));
    }

    @Test
    public void testValidationExceptionDeadTimerValueNotSet() throws Exception {
        try {
            createStateful02SessionInstance(null, (short) 100, true, true, true, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeadTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerNotSet() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, null, true, true, true, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("KeepAliveTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionStatefulNotSet() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, (short) 100, null, false, false, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Stateful value is not set"));
        }
    }

    @Test
    public void testValidationExceptionActiveNotSet() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, (short) 100, true, null, true, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Active value is not set"));
        }
    }

    @Test
    public void testValidationExceptionInstantiatedNotSet() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, (short) 100, true, true, null, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Initiated value is not set"));
        }
    }

    @Test
    public void testValidationExceptionTimeoutNotSet() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, (short) 100, true, true, true, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Timeout value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerMinValue() throws Exception {
        try {
            createStateful02SessionInstance((short) 200, (short) -10, true, true, true, 0);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("minimum value is 1."));
        }
    }

    @Test
    public void testStatefulAfterCommitted() throws Exception {
        createStateful02SessionInstance((short) 200, (short) 100, false, true, true, 0);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final Stateful02PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTROY_NAME, INSTANCE_NAME), Stateful02PCEPSessionProposalFactoryModuleMXBean.class);
        assertTrue(mxBean.getStateful());
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTROY_NAME);
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTROY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTROY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTROY_NAME);
        transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTROY_NAME, INSTANCE_NAME),
                Stateful02PCEPSessionProposalFactoryModuleMXBean.class);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTROY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createStateful02SessionProposalInstance(transaction);
        return transaction.commit();
    }

    private CommitStatus createStateful02SessionInstance(final Short deadTimer, final Short keepAlive, final Boolean isStateful,
            final Boolean isActive, final Boolean isInstant, final Integer timeout) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createStateful02SessionInstance(transaction, deadTimer, keepAlive, isStateful, isActive, isInstant, timeout);
        return transaction.commit();
    }

    public static ObjectName createStateful02SessionProposalInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createStateful02SessionInstance(transaction, (short) 200, (short) 100, true, true, true, 180);
    }

    private static ObjectName createStateful02SessionInstance(final ConfigTransactionJMXClient transaction, final Short deadTimer,
            final Short keepAlive, final Boolean stateful, final Boolean active, final Boolean instant, final Integer timeout) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTROY_NAME, INSTANCE_NAME);
        final Stateful02PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
                Stateful02PCEPSessionProposalFactoryModuleMXBean.class);
        mxBean.setActive(active);
        mxBean.setDeadTimerValue(deadTimer);
        mxBean.setInitiated(instant);
        mxBean.setKeepAliveTimerValue(keepAlive);
        mxBean.setStateful(stateful);
        mxBean.setTimeout(timeout);
        return nameCreated;
    }

}
