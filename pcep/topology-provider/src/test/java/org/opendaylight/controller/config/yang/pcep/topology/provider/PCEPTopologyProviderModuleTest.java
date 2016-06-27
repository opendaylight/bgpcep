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
import org.opendaylight.controller.config.yang.pcep.stateful07.cfg.PCEPStatefulCapabilityModuleFactory;
import org.opendaylight.controller.config.yang.programming.impl.AbstractInstructionSchedulerTest;
import org.opendaylight.controller.config.yang.tcpmd5.jni.cfg.NativeKeyAccessFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ServerChannelFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ServerChannelFactoryModuleMXBean;
import org.opendaylight.tcpmd5.jni.NativeTestSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class PCEPTopologyProviderModuleTest extends AbstractInstructionSchedulerTest {

    private static final String FACTORY_NAME = PCEPTopologyProviderModuleFactory.NAME;
    private static final String INSTANCE_NAME = "pcep-topology-provider-instance";
    private static final String STATEFUL07_TOPOLOGY_INSTANCE_NAME = "pcep-topology-stateful07-instance";

    private static final String LISTEN_ADDRESS = "0.0.0.0";
    private static final PortNumber LISTEN_PORT = new PortNumber(4189);
    private static final TopologyId TOPOLOGY_ID = new TopologyId("pcep-topology");

    @Test
    public void testValidationExceptionListenAddressNotSet() throws Exception {
        try {
            createInstance(null, LISTEN_PORT, TOPOLOGY_ID, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("ListenAddress is not set"));
        }
    }

    @Test
    public void testValidationExceptionListenPortNotSet() throws Exception {
        try {
            createInstance(LISTEN_ADDRESS, null, TOPOLOGY_ID, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("ListenPort is not set"));
        }
    }

    @Test
    public void testValidationExceptionTopologyIdNotSet() throws Exception {
        try {
            createInstance(LISTEN_ADDRESS, LISTEN_PORT, null, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("TopologyId is not set"));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance(false);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 17, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance(false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 17);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance(false);
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), PCEPTopologyProviderModuleMXBean.class);
        mxBean.setTopologyId(new TopologyId("new-pcep-topology"));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 16);
    }

    @Test
    public void testCreateBeanWithMD5() throws Exception {
        NativeTestSupport.assumeSupportedPlatform();
        final CommitStatus status = createInstance(true);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 19, 0, 0);
    }

    @Test
    public void testMD5ValidationFailure() throws Exception {
        NativeTestSupport.assumeSupportedPlatform();
        createInstance(true);
        // remove dispatcher's Md5ServerChannelFactory
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(
                transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), PCEPTopologyProviderModuleMXBean.class);
        final ObjectName dispatcherON = mxBean.getDispatcher();
        final PCEPDispatcherImplModuleMXBean dispatcher = transaction.newMXBeanProxy(dispatcherON, PCEPDispatcherImplModuleMXBean.class);
        dispatcher.setMd5ServerChannelFactory(null);
        try {
            transaction.validateConfig();
            fail();
        }catch(final ValidationException e){
            assertTrue(e.getMessage(), e.getMessage().contains("Client password is not compatible with selected dispatcher"));
        }
    }

    private CommitStatus createInstance(final String listenAddress, final PortNumber listenPort,
                                        final TopologyId topologyId, final boolean addMD5)
            throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createPCEPTopologyProviderModuleInstance(transaction, listenAddress, listenPort, topologyId, addMD5);
        return transaction.commit();
    }

    private CommitStatus createInstance(final boolean addMD5) throws Exception {
        return createInstance(LISTEN_ADDRESS, getRandomPortNumber(), TOPOLOGY_ID, addMD5);
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
        mxBean.setStatefulPlugin(transaction.createModule(Stateful07TopologySessionListenerModuleFactory.NAME,
                STATEFUL07_TOPOLOGY_INSTANCE_NAME));
        mxBean.setTopologyId(TOPOLOGY_ID);
        return objectName;
    }

    private static void addMd5(final ConfigTransactionJMXClient transaction, final PCEPTopologyProviderModuleMXBean mxBean) throws InstanceAlreadyExistsException {
        final ObjectName jniON = transaction.createModule(NativeKeyAccessFactoryModuleFactory.NAME, NativeKeyAccessFactoryModuleFactory.NAME);
        final ObjectName md5ServerChannelFactoryON = transaction.createModule(MD5ServerChannelFactoryModuleFactory.NAME, MD5ServerChannelFactoryModuleFactory.NAME);
        final MD5ServerChannelFactoryModuleMXBean md5Factory = transaction.newMXBeanProxy(md5ServerChannelFactoryON, MD5ServerChannelFactoryModuleMXBean.class);
        md5Factory.setServerKeyAccessFactory(jniON);


        final ObjectName dispatcherON = mxBean.getDispatcher();
        final PCEPDispatcherImplModuleMXBean dispatcher = transaction.newMXBeanProxy(dispatcherON, PCEPDispatcherImplModuleMXBean.class);
        dispatcher.setMd5ServerChannelFactory(md5ServerChannelFactoryON);
    }

    private ObjectName createPCEPTopologyProviderModuleInstance(final ConfigTransactionJMXClient transaction, final String listenAddress,
            final PortNumber listenPort, final TopologyId topologyId, final boolean addMD5) throws Exception {
        final ObjectName objectName = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        final ObjectName asyncDataBrokerON = createAsyncDataBrokerInstance(transaction);
        final ObjectName bindingBrokerON = createBindingBrokerImpl(transaction, createCompatibleDataBrokerInstance(transaction), notificationBrokerON);

        final PCEPTopologyProviderModuleMXBean mxBean = transaction.newMXBeanProxy(objectName, PCEPTopologyProviderModuleMXBean.class);
        mxBean.setDataProvider(asyncDataBrokerON);
        mxBean.setDispatcher(createDispatcherInstance(transaction, 5));

        if (addMD5) {
            addMd5(transaction, mxBean);
            // create 1 client
            final Client client = new Client();
            client.setPassword(Rfc2385Key.getDefaultInstance("foo"));
            client.setAddress(new IpAddress("127.0.0.1".toCharArray()));
            mxBean.setClient(Arrays.asList(client));
        }

        mxBean.setListenAddress(listenAddress == null ? null : new IpAddress(listenAddress.toCharArray()));
        mxBean.setListenPort(listenPort);
        mxBean.setRpcRegistry(bindingBrokerON);
        mxBean.setScheduler(createInstructionSchedulerModuleInstance(transaction, asyncDataBrokerON, bindingBrokerON,
                notificationBrokerON));
        mxBean.setStatefulPlugin(transaction.createModule(Stateful07TopologySessionListenerModuleFactory.NAME,
                STATEFUL07_TOPOLOGY_INSTANCE_NAME));
        mxBean.setTopologyId(topologyId);
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
        moduleFactories.add(new Stateful07TopologySessionListenerModuleFactory());
        moduleFactories.add(new PCEPStatefulCapabilityModuleFactory());
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
        paths.add("/META-INF/yang/network-topology-pcep.yang");
        paths.add("/META-INF/yang/network-topology-pcep-programming.yang");
        paths.add("/META-INF/yang/network-topology-programming.yang");
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
