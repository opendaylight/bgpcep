/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bmp.impl;

import java.math.BigDecimal;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bmp.spi.SimpleBmpExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bmp.spi.SimpleBmpExtensionProviderContextModuleMXBean;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleMXBean;
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyFactoryModuleFactory;
import org.opendaylight.controller.config.yang.protocol.framework.TimedReconnectStrategyFactoryModuleMXBean;

public class BmpDispatcherImplModuleTest extends AbstractConfigTest {
    private static final String INSTANCE_NAME = "bmp-message-fct";
    private static final String FACTORY_NAME = BmpDispatcherImplModuleFactory.NAME;

    private static final String BMP_EXTENSION_INSTANCE_NAME = "bmp-extension-impl";
    private static final String BOSS_TG_INSTANCE_NAME = "boss-threadgroup-impl";
    private static final String WORKER_TG_INSTANCE_NAME = "worker-threadgroup-impl";
    private static final String RECONNECT_STRATEGY_NAME = "timed-reconnect-strategy-factory";

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext,
            new BmpDispatcherImplModuleFactory(),
            new NettyThreadgroupModuleFactory(),
            new TimedReconnectStrategyFactoryModuleFactory(),
            new GlobalEventExecutorModuleFactory(),
            new SimpleBmpExtensionProviderContextModuleFactory()));
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 6, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 6);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createInstance(transaction);
        return transaction.commit();
    }

    public static ObjectName createInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BmpDispatcherImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BmpDispatcherImplModuleMXBean.class);
        mxBean.setBossGroup(createThreadgroupInstance(transaction, BOSS_TG_INSTANCE_NAME, 10));
        mxBean.setWorkerGroup(createThreadgroupInstance(transaction, WORKER_TG_INSTANCE_NAME, 10));
        mxBean.setBmpExtensions(createBmpExtensionsInstance(transaction));
        mxBean.setBmpReconnectStrategyFactory(createBmpReconnectStrategyFactoryInstance(transaction));
        return nameCreated;
    }

    private static ObjectName createThreadgroupInstance(final ConfigTransactionJMXClient transaction, final String instanceName,
            final Integer threadCount) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(NettyThreadgroupModuleFactory.NAME, instanceName);
        final NettyThreadgroupModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NettyThreadgroupModuleMXBean.class);
        mxBean.setThreadCount(threadCount);
        return nameCreated;
    }

    private static ObjectName createBmpExtensionsInstance(final ConfigTransactionJMXClient transaction)
            throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(SimpleBmpExtensionProviderContextModuleFactory.NAME, BMP_EXTENSION_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, SimpleBmpExtensionProviderContextModuleMXBean.class);
        return nameCreated;
    }

    private static ObjectName createGlobalEventExecutor(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(GlobalEventExecutorModuleFactory.NAME, GlobalEventExecutorModuleFactory.SINGLETON_NAME);
        return nameCreated;
    }

    private static ObjectName createBmpReconnectStrategyFactoryInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(TimedReconnectStrategyFactoryModuleFactory.NAME, RECONNECT_STRATEGY_NAME);
        final TimedReconnectStrategyFactoryModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, TimedReconnectStrategyFactoryModuleMXBean.class);
        mxBean.setConnectTime(720000);
        mxBean.setDeadline(720000L);
        mxBean.setMaxAttempts(10L);
        mxBean.setMaxSleep(30000L);
        mxBean.setMinSleep(30000L);
        mxBean.setSleepFactor(new BigDecimal(1.0));
        mxBean.setTimedReconnectExecutor(createGlobalEventExecutor(transaction));
        return nameCreated;
    }
}
