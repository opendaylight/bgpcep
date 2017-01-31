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

import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Collections;
import java.util.List;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleFactory;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleFactory;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleMXBean;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderModuleTest;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Stateful07TopologySessionListenerModuleFactory;
import org.opendaylight.controller.config.yang.programming.impl.AbstractInstructionSchedulerTest;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class PCEPTunnelTopologyProviderModuleTest extends AbstractInstructionSchedulerTest {

    private static final String FACTORY_NAME = PCEPTunnelTopologyProviderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "pcep-tunnel-topology-provider-instance";

    private static final TopologyId TOPOLOGY_ID = new TopologyId("pcep-topology");

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        SimplePCEPExtensionProviderContext extContext = new SimplePCEPExtensionProviderContext();
        setupMockService(PCEPExtensionProviderContext.class, extContext);
        BasePCEPSessionProposalFactory proposalFactory = new BasePCEPSessionProposalFactory(120, 30,
            Collections.singletonList(new PCEPStatefulCapability(true, true, true, true, true, true, true)));
        setupMockService(PCEPSessionProposalFactory.class, proposalFactory);
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        setupMockService(PCEPDispatcher.class, new PCEPDispatcherImpl(extContext.getMessageHandlerRegistry(),
                new DefaultPCEPSessionNegotiatorFactory(proposalFactory, 5), eventLoopGroup, eventLoopGroup));
    }

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
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
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
        assertStatus(status, 0, 1, 13);
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
        final ObjectName dataBrokerON = createCompatibleDataBrokerInstance(transaction);
        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        final ObjectName bindingBrokerON = createBindingBrokerImpl(transaction, dataBrokerON, notificationBrokerON);
        final ObjectName schedulerON = createInstructionSchedulerModuleInstance(transaction, asyncDataBrokerON, bindingBrokerON,
                notificationBrokerON);
        final ObjectName sourceTopology = PCEPTopologyProviderModuleTest.createPCEPTopologyProviderModuleInstance(transaction,
                asyncDataBrokerON, bindingBrokerON, schedulerON);

        final PCEPTunnelTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(objectName,
                PCEPTunnelTopologyProviderModuleMXBean.class);
        mxBean.setDataProvider(asyncDataBrokerON);
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
        moduleFactories.add(new NettyThreadgroupModuleFactory());
        moduleFactories.add(new Stateful07TopologySessionListenerModuleFactory());
        return moduleFactories;
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        paths.add("/META-INF/yang/network-topology-pcep.yang");
        paths.add("/META-INF/yang/network-topology-pcep-programming.yang");
        paths.add("/META-INF/yang/network-topology-programming.yang");
        paths.add("/META-INF/yang/topology-tunnel.yang");
        paths.add("/META-INF/yang/topology-tunnel-pcep.yang");
        paths.add("/META-INF/yang/topology-tunnel-pcep-programming.yang");
        paths.add("/META-INF/yang/topology-tunnel-p2p.yang");
        paths.add("/META-INF/yang/topology-tunnel-programming.yang");
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
