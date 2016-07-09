/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.evpn;

import com.google.common.collect.Lists;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleTest;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleTest;

public final class EvpnModuleTest extends AbstractConfigTest {
    private static final String FACTORY_NAME = EvpnModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bgp-evpn-impl";

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, new EvpnModuleFactory(),
            new SimpleBGPExtensionProviderContextModuleFactory(), new RIBExtensionsImplModuleFactory()));
    }

    @Test
    public void testLSTypeDefaultValue() throws Exception {
        final CommitStatus status = createEvpnModuleInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 3, 0, 0);
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createEvpnModuleInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 3, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createEvpnModuleInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 3);
    }

    @Test
    public void testReconfigureInstance() throws Exception {
        createEvpnModuleInstance();
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 3);
    }

    private CommitStatus createEvpnModuleInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        final ObjectName evpnON = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);

        SimpleBGPExtensionProviderContextModuleTest.createBGPExtensionsModuleInstance(transaction, Lists.newArrayList(evpnON));
        RIBExtensionsImplModuleTest.createRIBExtModuleInstance(transaction, Lists.newArrayList(evpnON));
        return transaction.commit();
    }

}