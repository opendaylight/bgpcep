/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.sr02.cfg;

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
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.Stateful07PCEPSessionProposalFactoryModuleMXBean;

public class Sr02PCEPSessionProposalFactoryModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "sr02-session-proposal";
    private static final String FACTORY_NAME = Sr02PCEPSessionProposalFactoryModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, new Sr02PCEPSessionProposalFactoryModuleFactory()));
    }

    @Test
    public void testValidationExceptionSrCapableValueNotSet() throws Exception {
        try {
            createInstance((short) 200, (short) 100, true, true, true, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("SrCapable value is not set"));
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
        transaction.newMBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                Stateful07PCEPSessionProposalFactoryModuleMXBean.class);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 1);
    }

    private CommitStatus createInstance() throws Exception {
        return createInstance((short) 200, (short) 100, true, true, true, true);
    }

    private CommitStatus createInstance(final Short deadTimer, final Short keepAlive,
            final Boolean stateful, final Boolean active, final Boolean instant, final Boolean srCapable) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction, deadTimer, keepAlive, stateful, active, instant, srCapable);
        return transaction.commit();
    }

    private ObjectName createInstance(final ConfigTransactionJMXClient transaction, final Short deadTimer, final Short keepAlive,
            final Boolean stateful, final Boolean active, final Boolean instant, final Boolean srCapable) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final Sr02PCEPSessionProposalFactoryModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                Sr02PCEPSessionProposalFactoryModuleMXBean.class);
        mxBean.setActive(active);
        mxBean.setDeadTimerValue(deadTimer);
        mxBean.setInitiated(instant);
        mxBean.setKeepAliveTimerValue(keepAlive);
        mxBean.setStateful(stateful);
        mxBean.setSrCapable(srCapable);
        return nameCreated;
    }
}
