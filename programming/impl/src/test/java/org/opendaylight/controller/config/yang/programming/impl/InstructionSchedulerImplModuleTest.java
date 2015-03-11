/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.programming.impl;

import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class InstructionSchedulerImplModuleTest extends AbstractInstructionSchedulerTest {
    private static final String FACTORY_NAME = InstructionSchedulerImplModuleFactory.NAME;

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 10, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 10);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        final ObjectName asyncDataBroker = createAsyncDataBrokerInstance(transaction);
        final ObjectName dataBrokerON = createCompatibleDataBrokerInstance(transaction);
        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        createInstructionSchedulerModuleInstance(transaction, asyncDataBroker , createBindingBrokerImpl(transaction,
                dataBrokerON, notificationBrokerON), notificationBrokerON);
        return transaction.commit();
    }
}