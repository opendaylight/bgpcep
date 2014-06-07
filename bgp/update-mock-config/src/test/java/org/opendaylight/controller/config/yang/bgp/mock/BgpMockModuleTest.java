/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.mock;

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
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusModuleMXBean;

public class BgpMockModuleTest extends AbstractConfigTest {

    private final String instanceName = "bgp-mock";

    private BgpMockModuleFactory factory;

    private EventBusModuleFactory eventBusFactory;

    @Before
    public void setUp() throws Exception {
        this.factory = new BgpMockModuleFactory();
        this.eventBusFactory = new EventBusModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.factory, this.eventBusFactory));
    }

    @Test
    public void testValidationExceptionBothAttributesSet() throws InstanceAlreadyExistsException {
        try {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            createInstance(transaction, this.factory.getImplementationName(), instanceName, new byte[] { 1 }, "hexMsg",
                    this.eventBusFactory.getImplementationName());
            transaction.validateConfig();
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Both 'HexDump' and 'BinDump' contain value"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, this.factory.getImplementationName(), instanceName, null, "hexString",
                this.eventBusFactory.getImplementationName());
        transaction.validateConfig();
        CommitStatus status = transaction.commit();
        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 2, 0, 0);
    }

    public static ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String moduleName,
            final String instanceName, final byte[] binDump, final String hexDump, final String eventBusImplName)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(moduleName, instanceName);
        BgpMockModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated, BgpMockModuleMXBean.class);
        mxBean.setBinDump(binDump);
        mxBean.setHexDump(hexDump);
        mxBean.setEventBus(createEventBus(transaction, eventBusImplName, "event-bus1"));
        return nameCreated;
    }

    public static ObjectName createEventBus(final ConfigTransactionJMXClient transaction, final String moduleName, final String instanceName)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(moduleName, instanceName);
        transaction.newMBeanProxy(nameCreated, EventBusModuleMXBean.class);
        return nameCreated;
    }

}
