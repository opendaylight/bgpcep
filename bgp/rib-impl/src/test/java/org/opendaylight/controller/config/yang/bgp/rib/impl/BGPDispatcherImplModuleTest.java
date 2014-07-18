/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;
import org.opendaylight.controller.config.yang.netty.timer.HashedWheelTimerModuleFactory;

public class BGPDispatcherImplModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "bgp-message-fct";
    private static final String FACTORY_NAME = BGPDispatcherImplModuleFactory.NAME;

    private static final String BGP_EXTENSION_INSTANCE_NAME = "bgp-extension-impl";
    private static final String BOSS_TG_INSTANCE_NAME = "boss-threadgroup-impl";
    private static final String WORKER_TG_INSTANCE_NAME = "worker-threadgroup-impl";

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, new BGPDispatcherImplModuleFactory(), new NettyThreadgroupModuleFactory(), new RIBExtensionsImplModuleFactory(), new SimpleBGPExtensionProviderContextModuleFactory(), new HashedWheelTimerModuleFactory()));
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 4, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 4);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction);
        return transaction.commit();
    }

    public static ObjectName createInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BGPDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPDispatcherImplModuleMXBean.class);
        mxBean.setBossGroup(createThreadgroupInstance(transaction, BOSS_TG_INSTANCE_NAME, 10));
        mxBean.setWorkerGroup(createThreadgroupInstance(transaction, WORKER_TG_INSTANCE_NAME, 10));
        mxBean.setBgpExtensions(createBgpExtensionsInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createThreadgroupInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
            final Integer threadCount) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(NettyThreadgroupModuleFactory.NAME, instanceName);
        final NettyThreadgroupModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(threadCount);
        return nameCreated;
    }

    private static ObjectName createBgpExtensionsInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(SimpleBGPExtensionProviderContextModuleFactory.NAME, BGP_EXTENSION_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, SimpleBGPExtensionProviderContextModuleMXBean.class);
        return nameCreated;
    }
}
