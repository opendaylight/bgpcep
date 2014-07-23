/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.tunnel.provider;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleFactory;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPSessionProposalFactoryImplModuleFactory;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.pcep.stateful02.cfg.Stateful02PCEPSessionProposalFactoryModuleFactory;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleFactory;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleMXBean;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleTest;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Stateful02TopologySessionListenerModuleFactory;
import org.opendaylight.controller.config.yang.programming.impl.AbstractInstructionSchedulerTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class PCEPTunnelTopologyProviderModuleTest extends AbstractInstructionSchedulerTest {

    private static final String FACTORY_NAME = PCEPTunnelTopologyProviderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "pcep-tunnel-topology-provider-instance";

    private static final TopologyId TOPOLOGY_ID = new TopologyId("pcep-topology");

    @Test
    public void testValidationExceptionTopologyIdNotSet() throws Exception {
        try {
            createInstance(null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 19, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 19);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), PCEPTopologyProviderModuleMXBean.class);
        mxBean.setTopologyId(new TopologyId("new-pcep-topology"));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 18);
    }

    private CommitStatus createInstance(final TopologyId topologyId) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createPCEPTopologyProviderModuleInstance(transaction, topologyId);
        return transaction.commit();
    }

    private CommitStatus createInstance() throws Exception {
        return createInstance(TOPOLOGY_ID);
    }

    private ObjectName createPCEPTopologyProviderModuleInstance(final ConfigTransactionJMXClient transaction, final TopologyId topologyId)
            throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final ObjectName asyncDataBrokerON = createAsyncDataBrokerInstance(transaction);
        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        final ObjectName bindingBrokerON = createBindingBrokerImpl(transaction, createCompatibleDataBrokerInstance(transaction), notificationBrokerON);
        final ObjectName schedulerON = createInstructionSchedulerModuleInstance(transaction, asyncDataBrokerON, bindingBrokerON,
                notificationBrokerON);
        final ObjectName sourceTopology = PCEPTopologyProviderModuleTest.createPCEPTopologyProviderModuleInstance(transaction,
                asyncDataBrokerON, bindingBrokerON, schedulerON);

        final PCEPTunnelTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(objectName,
                PCEPTunnelTopologyProviderModuleMXBean.class);
        mxBean.setDataProvider(createDataBrokerInstance(transaction));
        mxBean.setRpcRegistry(bindingBrokerON);
        mxBean.setScheduler(schedulerON);
        mxBean.setTopologyId(topologyId);
        mxBean.setSourceTopology(sourceTopology);
        return objectName;
    }

    @Override
    public List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new PCEPTunnelTopologyProviderModuleFactory());
        moduleFactories.add(new PCEPTopologyProviderModuleFactory());
        moduleFactories.add(new PCEPDispatcherImplModuleFactory());
        moduleFactories.add(new PCEPSessionProposalFactoryImplModuleFactory());
        moduleFactories.add(new NettyThreadgroupModuleFactory());
        moduleFactories.add(new SimplePCEPExtensionProviderContextModuleFactory());
        moduleFactories.add(new Stateful02TopologySessionListenerModuleFactory());
        moduleFactories.add(new Stateful02PCEPSessionProposalFactoryModuleFactory());
        return moduleFactories;
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/network-topology-pcep.yang");
        paths.add("/META-INF/yang/odl-network-topology.yang");
        paths.add("/META-INF/yang/yang-ext.yang");
        paths.add("/META-INF/yang/pcep-types.yang");
        paths.add("/META-INF/yang/rsvp.yang");
        paths.add("/META-INF/yang/iana.yang");
        paths.add("/META-INF/yang/network-concepts.yang");
        paths.add("/META-INF/yang/ieee754.yang");
        return paths;
    }
}
