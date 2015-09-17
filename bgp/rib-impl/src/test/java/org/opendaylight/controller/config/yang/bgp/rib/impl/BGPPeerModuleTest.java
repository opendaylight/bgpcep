/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.tcpmd5.jni.cfg.NativeKeyAccessFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ClientChannelFactoryModuleFactory;
import org.opendaylight.controller.config.yang.tcpmd5.netty.cfg.MD5ClientChannelFactoryModuleMXBean;
import org.opendaylight.tcpmd5.jni.NativeTestSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;

public class BGPPeerModuleTest extends AbstractRIBImplModuleTest {

    private static final String INSTANCE_NAME = "bgp-peer-module-impl";
    private static final String FACTORY_NAME = BGPPeerModuleFactory.NAME;

    private static final String HOST = "127.0.0.1";
    private static final PortNumber portNumber = new PortNumber(1);

    @Override
    protected BindingRuntimeContext getBindingRuntimeContext() {
        final BindingRuntimeContext ret = super.getBindingRuntimeContext();
        doReturn(Ipv4AddressFamily.class).when(ret).getIdentityClass(Ipv4AddressFamily.QNAME);
        doReturn(MplsLabeledVpnSubsequentAddressFamily.class).when(ret).getIdentityClass(MplsLabeledVpnSubsequentAddressFamily.QNAME);
        return ret;
    }

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new BGPPeerModuleFactory());
        moduleFactories.add(new BGPTableTypeImplModuleFactory());
        moduleFactories.add(new NativeKeyAccessFactoryModuleFactory());
        moduleFactories.add(new MD5ClientChannelFactoryModuleFactory());
        moduleFactories.add(new StrictBgpPeerRegistryModuleFactory());
        return moduleFactories;
    }

    @Test
    public void testValidationExceptionPortNotSet() throws Exception {
        try {
            createBgpPeerInstance(HOST, null, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Port value is not set."));
        }
    }

    @Test
    public void testValidationExceptionInternalPeerRole() throws Exception {
        try {
            createInternalBgpPeerInstance();
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Internal Peer Role is reserved for Application Peer use."));
        }
    }

    @Test
    public void testValidationExceptionHostNotSet() throws Exception {
        try {
            createBgpPeerInstance(null, portNumber, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Host value is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createBgpPeerInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 16, 0, 0);
    }

    @Test
    public void testCreateBeanWithMD5() throws Exception {
        NativeTestSupport.assumeSupportedPlatform();
        final CommitStatus status = createBgpPeerInstance(true);
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 18, 0, 0);
    }

    @Test
    public void testMD5ValidationFailure() throws Exception {
        NativeTestSupport.assumeSupportedPlatform();
        createBgpPeerInstance(true);
        // now remove md5 from dispatcher
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName nameCreated = transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPPeerModuleMXBean.class);
        final BGPDispatcherImplModuleMXBean bgpDispatcherImplModuleMXBean = getBgpDispatcherImplModuleMXBean(transaction, mxBean);
        bgpDispatcherImplModuleMXBean.setMd5ChannelFactory(null);
        try {
            transaction.validateConfig();
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Underlying dispatcher does not support MD5 clients"));
        }
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        CommitStatus status = createBgpPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 16);
    }

    @Test
    public void testReconfigure() throws Exception {
        CommitStatus status = createBgpPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPPeerModuleMXBean.class);
        mxBean.setPort(new PortNumber(10));
        status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 15);
    }

    private ObjectName createBgpPeerInstance(final ConfigTransactionJMXClient transaction, final String host,
            final PortNumber port, final boolean md5, final boolean internalPeerRole) throws Exception {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPPeerModuleMXBean.class);

        mxBean.setPeerRegistry(createPeerRegistry(transaction));

        // FIXME JMX crashes if union was not created via artificial constructor - Bug:1276
        // annotated for JMX as value
        // IpAddress host1 = new IpAddress(new Ipv4Address(host));
        mxBean.setHost(host == null ? null : new IpAddress(host.toCharArray()));
        mxBean.setPort(port);
        mxBean.setAdvertizedTable(Collections.<ObjectName> emptyList());
        {
            final ObjectName ribON = createRIBImplModuleInstance(transaction);
            mxBean.setRib(ribON);
        }
        if (md5) {
            final BGPDispatcherImplModuleMXBean bgpDispatcherProxy = getBgpDispatcherImplModuleMXBean(transaction, mxBean);
            final ObjectName jniON = transaction.createModule(NativeKeyAccessFactoryModuleFactory.NAME, NativeKeyAccessFactoryModuleFactory.NAME);
            final ObjectName md5ClientON = transaction.createModule(MD5ClientChannelFactoryModuleFactory.NAME,
                    MD5ClientChannelFactoryModuleFactory.NAME);
            final MD5ClientChannelFactoryModuleMXBean md5ClientProxy =
                    transaction.newMXBeanProxy(md5ClientON, MD5ClientChannelFactoryModuleMXBean.class);
            md5ClientProxy.setKeyAccessFactory(jniON);

            bgpDispatcherProxy.setMd5ChannelFactory(md5ClientON);

            mxBean.setPassword(Rfc2385Key.getDefaultInstance("foo"));

        }

        if(internalPeerRole) {
            mxBean.setPeerRole(PeerRole.Internal);
        }

        mxBean.setAdvertizedTable(Lists.newArrayList(BGPTableTypeImplModuleTest.createTableInstance(transaction,
                new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString()),
                new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString()))));
        return nameCreated;
    }

    private static ObjectName createPeerRegistry(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        return transaction.createModule(StrictBgpPeerRegistryModuleFactory.NAME, "peer-registry");
    }

    private static BGPDispatcherImplModuleMXBean getBgpDispatcherImplModuleMXBean(final ConfigTransactionJMXClient transaction,
            final BGPPeerModuleMXBean mxBean) {
        final RIBImplModuleMXBean ribProxy = transaction.newMXBeanProxy(mxBean.getRib(), RIBImplModuleMXBean.class);
        final ObjectName dispatcherON = ribProxy.getBgpDispatcher();
        return transaction.newMXBeanProxy(dispatcherON, BGPDispatcherImplModuleMXBean.class);
    }

    private CommitStatus createBgpPeerInstance() throws Exception {
        return createBgpPeerInstance(false);
    }

    private CommitStatus createBgpPeerInstance(final boolean md5) throws Exception {
        return createBgpPeerInstance(HOST, portNumber, md5);
    }

    private CommitStatus createBgpPeerInstance(final String host, final PortNumber port, final boolean md5) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createBgpPeerInstance(transaction, host, port, md5, false);
        return transaction.commit();
    }

    private CommitStatus createInternalBgpPeerInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createBgpPeerInstance(transaction, HOST, portNumber, false, true);
        return transaction.commit();
    }
}
