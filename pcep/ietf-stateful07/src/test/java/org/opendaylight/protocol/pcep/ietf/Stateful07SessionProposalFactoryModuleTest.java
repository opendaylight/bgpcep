/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertFalse;
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
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.Stateful07PCEPSessionProposalFactoryModuleFactory;
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.Stateful07PCEPSessionProposalFactoryModuleMXBean;

public class Stateful07SessionProposalFactoryModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "pcep-stateful07-proposal";
    private static final String FACTORY_NAME = Stateful07PCEPSessionProposalFactoryModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, new Stateful07PCEPSessionProposalFactoryModuleFactory()));
    }

    @Test
    public void testValidationExceptionDeadTimerValueNotSet() throws Exception {
        try {
            createInstance(null, (short) 100, true, true, true, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeadTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerNotSet() throws Exception {
        try {
            createInstance((short) 200, null, true, true, true, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("KeepAliveTimerValue value is not set"));
        }
    }

    @Test
    public void testValidationExceptionStatefulNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, null, false, false, false, false, false, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Stateful value is not set"));
        }
    }

    @Test
    public void testValidationExceptionActiveNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, null, true, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Active value is not set"));
        }
    }

    @Test
    public void testValidationExceptionInstantiatedNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, null, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Initiated value is not set"));
        }
    }

    @Test
    public void testValidationExceptionTriggeredSyncNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, true, null, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TriggeredInitialSync value is not set"));
        }
    }

    @Test
    public void testValidationExceptionTriggeredResyncNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, true, true, null, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TriggeredResync value is not set"));
        }
    }

    @Test
    public void testValidationExceptionDeltaLspSyncNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, true, true, true, null, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("DeltaLspSyncCapability value is not set"));
        }
    }

    @Test
    public void testValidationExceptionIncludeDBVersionNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, true, true, true, true, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("IncludeDbVersion value is not set"));
        }
    }

    @Test
    public void testValidationExceptionKeepAliveTimerMinValue() throws Exception {
        try {
            createInstance((short) 200, (short) -10, true, true, true, true, true, true, true);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("minimum value is 1."));
        }
    }

    @Test
    public void testStatefulAfterCommitted() throws Exception {
        createInstance((short) 200, (short) 100, false, true, true, true, false, true, true);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final Stateful07PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        assertTrue(mxBean.getStateful());
    }

    @Test
    public void testNotStatefulAfterCommitted() throws Exception {
        createInstance((short) 200, (short) 100, false, false, false, false, false, false, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final Stateful07PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        assertFalse(mxBean.getStateful());
    }

    @Test
    public void testIncludeDbVersionAfterCommitted() throws Exception {
        createInstance((short) 200, (short) 100, false, false, false, false, false, true, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final Stateful07PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        assertTrue(mxBean.getIncludeDbVersion());
    }

    @Test
    public void testNotIncludeDbVersionAfterCommitted() throws Exception {
        createInstance((short) 200, (short) 100, false, false, false, false, false, false, false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final Stateful07PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(
                FACTORY_NAME, INSTANCE_NAME), Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        assertFalse(mxBean.getIncludeDbVersion());
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
        transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    private CommitStatus createInstance(final Short deadTimer, final Short keepAlive, final Boolean stateful, final Boolean active,
            final Boolean instant, final Boolean triggeredInitialSync, final Boolean triggeredResync, final Boolean deltaLspSyncCapability, final Boolean includeDbVersion) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction, deadTimer, keepAlive, stateful, active, instant, triggeredInitialSync, triggeredResync, deltaLspSyncCapability, includeDbVersion);
        return transaction.commit();
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createStateful07SessionProposalInstance(transaction);
        return transaction.commit();
    }

    public static ObjectName createStateful07SessionProposalInstance(final ConfigTransactionJMXClient transaction) throws Exception {
        return createInstance(transaction, (short) 200, (short) 100, true, true, true, true, true, true, true);
    }

    private static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final Short deadTimer, final Short keepAlive,
            final Boolean stateful, final Boolean active, final Boolean instant,
            final Boolean triggeredInitialSync, final Boolean triggeredResync, final Boolean deltaLspSyncCapability, final Boolean includeDbVersion) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final Stateful07PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated,
                Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        mxBean.setActive(active);
        mxBean.setDeadTimerValue(deadTimer);
        mxBean.setInitiated(instant);
        mxBean.setKeepAliveTimerValue(keepAlive);
        mxBean.setStateful(stateful);
        mxBean.setTriggeredInitialSync(triggeredInitialSync);
        mxBean.setTriggeredResync(triggeredResync);
        mxBean.setDeltaLspSyncCapability(deltaLspSyncCapability);
        mxBean.setIncludeDbVersion(includeDbVersion);
        return nameCreated;
    }

}
