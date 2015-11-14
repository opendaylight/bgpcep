/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.AbstractMockedModule;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.netty.threadgroup.NettyThreadgroupModuleFactory;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

public class BGPPeerAcceptorModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "bgp-peer-acceptor";
    private static final String FACTORY_NAME = BGPPeerAcceptorModuleFactory.NAME;

    @Before
    public void setUp() throws Exception {
        final List<ModuleFactory> moduleFactories = getModuleFactories();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext, moduleFactories.toArray(new ModuleFactory[moduleFactories.size()])));
    }

    private List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = Lists.newArrayList();
        moduleFactories.add(new StrictBgpPeerRegistryModuleFactory());
        moduleFactories.add(new BGPPeerAcceptorModuleFactory());
        moduleFactories.add(new NettyThreadgroupModuleFactory());
        moduleFactories.add(new SimpleBGPExtensionProviderContextModuleFactory());
        moduleFactories.add(createClassBasedCBF(MockedDispatcherModule.class, "dispatch"));
        return moduleFactories;
    }

    @Test
    public void testCreateBeanDefaultAddress() throws InstanceAlreadyExistsException, ConflictingVersionException, ValidationException {
        try {
            final CommitStatus status = createRegistryInstance(Optional.<String>absent(), Optional.<Integer>absent(), true, true);
            assertBeanCount(1, FACTORY_NAME);
            assertStatus(status, 3, 0, 0);
            verify(dispatcher).createServer(any(BGPPeerRegistry.class), any(InetSocketAddress.class));
        } catch (final ValidationException e) {
            if(!PlatformDependent.isWindows() && !PlatformDependent.isRoot()) {
                Assert.assertTrue(e.getMessage().contains("Unable to bind port"));
            } else {
                fail();
            }
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createRegistryInstance(Optional.of("127.0.0.1"), Optional.of(1790), true, true);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 3, 0, 0);
        verify(dispatcher).createServer(any(BGPPeerRegistry.class), any(InetSocketAddress.class));
    }

    private CommitStatus createRegistryInstance(final Optional<String> address, final Optional<Integer> port, final boolean addRegistry, final boolean addDispatcher ) throws InstanceAlreadyExistsException, ValidationException, ConflictingVersionException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName module = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BGPPeerAcceptorModuleMXBean proxy = transaction.newMXBeanProxy(module, BGPPeerAcceptorModuleMXBean.class);

        // FIXME JMX crashes if union was not created via artificial constructor - Bug:1276
        if(address.isPresent()) {
            proxy.setBindingAddress(new IpAddress(address.get().toCharArray()));
        }
        if(port.isPresent()) {
            proxy.setBindingPort(new PortNumber(port.get()));
        }
        if(addRegistry) {
            proxy.setAcceptingPeerRegistry(createPeerRegistry(transaction));
        }
        if(addDispatcher) {
            proxy.setAcceptingBgpDispatcher(createDispatcher(transaction));
        }
        return transaction.commit();
    }

    private static ObjectName createPeerRegistry(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        return transaction.createModule(StrictBgpPeerRegistryModuleFactory.NAME, "peer-registry");
    }

    private static ObjectName createDispatcher(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        return transaction.createModule("dispatch", "mock");
    }

    private static interface MockDispatcher extends BGPDispatcher, AutoCloseable {}

    @Mock
    static MockDispatcher dispatcher;

    @Before
    public void setUpMockDispatcher() throws Exception {
        MockitoAnnotations.initMocks(BGPPeerAcceptorModuleTest.this);
        final ChannelFuture future = mock(ChannelFuture.class);
        doReturn(true).when(future).cancel(anyBoolean());
        final Channel channel = mock(Channel.class);
        doReturn(mock(ChannelFuture.class)).when(channel).close();
        doReturn(channel).when(future).channel();
        doReturn(mock(ChannelFuture.class)).when(future).addListener(any(GenericFutureListener.class));
        doReturn(future).when(dispatcher).createServer(any(BGPPeerRegistry.class), any(InetSocketAddress.class));
        doNothing().when(dispatcher).close();
    }

    public final static class MockedDispatcherModule extends AbstractMockedModule implements BGPDispatcherImplModuleMXBean, BGPDispatcherServiceInterface {

        public MockedDispatcherModule(final DynamicMBeanWithInstance old, final ModuleIdentifier id) {
            super(old, id);
        }

        @Override
        protected AutoCloseable prepareMockedInstance() throws Exception {return dispatcher;}

        @Override
        public ObjectName getWorkerGroup() {return null;}

        @Override
        public void setWorkerGroup(final ObjectName workerGroup) {}

        @Override
        public ObjectName getBgpExtensions() {return null;}

        @Override
        public void setBgpExtensions(final ObjectName bgpExtensions) {}

        @Override
        public ObjectName getMd5ChannelFactory() {return null;}

        @Override
        public void setMd5ChannelFactory(final ObjectName md5ChannelFactory) {}

        @Override
        public ObjectName getBossGroup() {return null;}

        @Override
        public void setBossGroup(final ObjectName bossGroup) {}

        @Override
        public ObjectName getMd5ServerChannelFactory() {return null;}

        @Override
        public void setMd5ServerChannelFactory(final ObjectName md5ServerChannelFactory) {}
    }
}
