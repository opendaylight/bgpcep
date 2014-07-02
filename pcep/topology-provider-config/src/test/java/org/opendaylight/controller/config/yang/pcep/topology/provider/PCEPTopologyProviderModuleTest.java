/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.topology.provider;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleTest.createDispatcherInstance;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleFactory;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleMXBean;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPSessionProposalFactoryImplModuleFactory;
import org.opendaylight.controller.config.yang.pcep.spi.SimplePCEPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.pcep.stateful02.cfg.Stateful02PCEPSessionProposalFactoryModuleFactory;
import org.opendaylight.controller.config.yang.programming.impl.AbstractInstructionSchedulerTest;
import org.opendaylight.controller.config.yang.tcpmd5.jni.cfg.NativeKeyAccessFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ServerChannelFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ServerChannelFactoryModuleMXBean;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class PCEPTopologyProviderModuleTest extends AbstractInstructionSchedulerTest {

    private static final String FACTORY_NAME = PCEPTopologyProviderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "pcep-topology-provider-instance";
    private static final String STATEFUL02_TOPOLOGY_INSTANCE_NAME = "pcep-topology-stateful02-instance";

    private static final String LISTEN_ADDRESS = "0.0.0.0";
    private static final PortNumber LISTEN_PORT = new PortNumber(4189);
    private static final TopologyId TOPOLOGY_ID = new TopologyId("pcep-topology");

    @Test
    public void testValidationExceptionListenAddressNotSet() throws Exception {
        try {
            createInstance(null, LISTEN_PORT, TOPOLOGY_ID);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("ListenAddress is not set"));
        }
    }

    @Test
    public void testValidationExceptionListenPortNotSet() throws Exception {
        try {
            createInstance(LISTEN_ADDRESS, null, TOPOLOGY_ID);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("ListenPort is not set"));
        }
    }

    @Test
    public void testValidationExceptionTopologyIdNotSet() throws Exception {
        try {
            createInstance(LISTEN_ADDRESS, LISTEN_PORT, null);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 18, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 18);
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
        assertStatus(status, 0, 1, 17);
    }

    @Test
    public void testMd5ValidationFailure() throws Exception {
        createInstance();
        // remove dispatcher's Md5ServerChannelFactory
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), PCEPTopologyProviderModuleMXBean.class);
        ObjectName dispatcherON = mxBean.getDispatcher();
        PCEPDispatcherImplModuleMXBean dispatcher = transaction.newMXBeanProxy(dispatcherON, PCEPDispatcherImplModuleMXBean.class);
        dispatcher.setMd5ServerChannelFactory(null);
        try {
            transaction.validateConfig();
            fail();
        }catch(ValidationException e){
            assertTrue(e.getMessage(), e.getMessage().contains("Client password is not compatible with selected dispatcher"));
        }
    }

    private CommitStatus createInstance(final String listenAddress, final PortNumber listenPort, final TopologyId topologyId)
            throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createPCEPTopologyProviderModuleInstance(transaction, listenAddress, listenPort, topologyId);
        return transaction.commit();
    }

    private CommitStatus createInstance() throws Exception {
        return createInstance(LISTEN_ADDRESS, getRandomPortNumber(), TOPOLOGY_ID);
    }

    public static ObjectName createPCEPTopologyProviderModuleInstance(final ConfigTransactionJMXClient transaction,
            final ObjectName dataBrokerON, final ObjectName bindingBrokerON, final ObjectName schedulerON) throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(objectName, PCEPTopologyProviderModuleMXBean.class);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setDispatcher(createDispatcherInstance(transaction, 5));

        mxBean.setListenAddress(new IpAddress(LISTEN_ADDRESS.toCharArray()));
        mxBean.setListenPort(getRandomPortNumber());
        mxBean.setRpcRegistry(bindingBrokerON);
        mxBean.setScheduler(schedulerON);
        mxBean.setStatefulPlugin(transaction.createModule(Stateful02TopologySessionListenerModuleFactory.NAME,
                STATEFUL02_TOPOLOGY_INSTANCE_NAME));
        mxBean.setTopologyId(TOPOLOGY_ID);
        return objectName;
    }

    public static void addMd5(ConfigTransactionJMXClient transaction, PCEPTopologyProviderModuleMXBean mxBean) throws InstanceAlreadyExistsException {
        ObjectName jniON = transaction.createModule(NativeKeyAccessFactoryModuleFactory.NAME, NativeKeyAccessFactoryModuleFactory.NAME);
        ObjectName md5ServerChannelFactoryON = transaction.createModule(MD5ServerChannelFactoryModuleFactory.NAME, MD5ServerChannelFactoryModuleFactory.NAME);
        MD5ServerChannelFactoryModuleMXBean md5Factory = transaction.newMXBeanProxy(md5ServerChannelFactoryON, MD5ServerChannelFactoryModuleMXBean.class);
        md5Factory.setServerKeyAccessFactory(jniON);


        ObjectName dispatcherON = mxBean.getDispatcher();
        PCEPDispatcherImplModuleMXBean dispatcher = transaction.newMXBeanProxy(dispatcherON, PCEPDispatcherImplModuleMXBean.class);
        dispatcher.setMd5ServerChannelFactory(md5ServerChannelFactoryON);
    }

    private ObjectName createPCEPTopologyProviderModuleInstance(final ConfigTransactionJMXClient transaction, final String listenAddress,
            final PortNumber listenPort, final TopologyId topologyId) throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final ObjectName dataBrokerON = createDataBrokerInstance(transaction);
        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        final ObjectName bindingBrokerON = createBindingBrokerImpl(transaction, dataBrokerON, notificationBrokerON);

        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(objectName, PCEPTopologyProviderModuleMXBean.class);
        mxBean.setDataProvider(dataBrokerON);
        mxBean.setDispatcher(createDispatcherInstance(transaction, 5));

        addMd5(transaction, mxBean);

        mxBean.setListenAddress(listenAddress == null ? null : new IpAddress(listenAddress.toCharArray()));
        mxBean.setListenPort(listenPort);
        mxBean.setRpcRegistry(bindingBrokerON);
        mxBean.setScheduler(createInstructionSchedulerModuleInstance(transaction, dataBrokerON, bindingBrokerON, notificationBrokerON));
        mxBean.setStatefulPlugin(transaction.createModule(Stateful02TopologySessionListenerModuleFactory.NAME,
                STATEFUL02_TOPOLOGY_INSTANCE_NAME));
        mxBean.setTopologyId(topologyId);

        // create 1 client
        Client client = new Client();
        client.setPassword(Rfc2385Key.getDefaultInstance("foo"));
        client.setAddress(new IpAddress("127.0.0.1".toCharArray()));
        mxBean.setClient(Arrays.asList(client));

        return objectName;
    }

    @Override
    public List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new PCEPTopologyProviderModuleFactory());
        moduleFactories.add(new PCEPDispatcherImplModuleFactory());
        moduleFactories.add(new PCEPSessionProposalFactoryImplModuleFactory());
        moduleFactories.add(new NettyThreadgroupModuleFactory());
        moduleFactories.add(new SimplePCEPExtensionProviderContextModuleFactory());
        moduleFactories.add(new Stateful02TopologySessionListenerModuleFactory());
        moduleFactories.add(new Stateful02PCEPSessionProposalFactoryModuleFactory());
        moduleFactories.add(new NativeKeyAccessFactoryModuleFactory());
        moduleFactories.add(new MD5ServerChannelFactoryModuleFactory());
        return moduleFactories;
    }

    private static PortNumber getRandomPortNumber() {
        final Random random = new Random();
        return new PortNumber(random.nextInt(65000 - 30000 + 1) + 30000);
    }

    @Override
    public List<String> getYangModelsPaths() {
        final List<String> paths = super.getYangModelsPaths();
        paths.add("/META-INF/yang/network-topology@2013-10-21.yang");
        return paths;
    }
}
