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
import io.netty.channel.epoll.Epoll;
import java.util.ArrayList;
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
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;

public class BGPPeerModuleTest extends AbstractRIBImplModuleTest {

    private static final int EXP_INSTANCES = 18;
    private static final String INSTANCE_NAME = "bgp-peer-module-impl";
    private static final String FACTORY_NAME = BGPPeerModuleFactory.NAME;

    private static final IpAddress HOST = new IpAddress(new Ipv4Address("127.0.0.1"));
    private static final PortNumber PORT_NUMBER = new PortNumber(1);

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
        moduleFactories.add(new StrictBgpPeerRegistryModuleFactory());
        moduleFactories.add(new AddPathImplModuleFactory());
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
            createBgpPeerInstance(null, PORT_NUMBER, false);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Host value is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createBgpPeerInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, EXP_INSTANCES, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        CommitStatus status = createBgpPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, EXP_INSTANCES);
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
        assertStatus(status, 0, 1, EXP_INSTANCES - 1);
    }

    private ObjectName createBgpPeerInstance(final ConfigTransactionJMXClient transaction, final IpAddress host,
            final PortNumber port, final boolean md5, final boolean internalPeerRole) throws Exception {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPPeerModuleMXBean.class);

        mxBean.setPeerRegistry(createPeerRegistry(transaction));
        mxBean.setHost(host);
        mxBean.setPort(port);
        mxBean.setAdvertizedTable(Collections.emptyList());
        mxBean.setRouteRefresh(false);
        {
            final ObjectName ribON = createRIBImplModuleInstance(transaction);
            mxBean.setRib(ribON);
        }
        mxBean.setAddPath(createAddPathCollection(transaction));
        if (Epoll.isAvailable()) {
            mxBean.setPassword(Rfc2385Key.getDefaultInstance("foo"));
        }
        if (internalPeerRole) {
            mxBean.setPeerRole(PeerRole.Internal);
        }

        mxBean.setAdvertizedTable(Lists.newArrayList(BGPTableTypeImplModuleTest.createTableInstance(transaction,
            new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString()),
            new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString()))));

        final ObjectName notificationBrokerON = createNotificationBrokerInstance(transaction);
        final ObjectName bindingBrokerON = createBindingBrokerImpl(transaction, createCompatibleDataBrokerInstance(transaction), notificationBrokerON);
        mxBean.setRpcRegistry(bindingBrokerON);
        return nameCreated;
    }

    private static List<ObjectName> createAddPathCollection(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName name1 = transaction.createModule(AddPathImplModuleFactory.NAME, "add-path-inst-1");
        final AddPathImplModuleMXBean mxBean = transaction.newMXBeanProxy(name1, AddPathImplModuleMXBean.class);
        mxBean.setAddressFamily(AddPathImplModuleTest.createAddressFamily(transaction, "add-path-inst-1"));
        mxBean.setSendReceive(SendReceive.Both);

        final ObjectName name2 = transaction.createModule(AddPathImplModuleFactory.NAME, "add-path-inst-2");
        final AddPathImplModuleMXBean mxBean2 = transaction.newMXBeanProxy(name2, AddPathImplModuleMXBean.class);
        mxBean2.setAddressFamily(AddPathImplModuleTest.createAddressFamily(transaction, "add-path-inst-2"));
        mxBean2.setSendReceive(SendReceive.Receive);

        final List<ObjectName> ret = new ArrayList<ObjectName>();
        ret.add(name1);
        ret.add(name2);
        return ret;
    }

    private static ObjectName createPeerRegistry(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        return transaction.createModule(StrictBgpPeerRegistryModuleFactory.NAME, "peer-registry");
    }

    private CommitStatus createBgpPeerInstance() throws Exception {
        return createBgpPeerInstance(false);
    }

    private CommitStatus createBgpPeerInstance(final boolean md5) throws Exception {
        return createBgpPeerInstance(HOST, PORT_NUMBER, md5);
    }

    private CommitStatus createBgpPeerInstance(final IpAddress host, final PortNumber port, final boolean md5) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createBgpPeerInstance(transaction, host, port, md5, false);
        return transaction.commit();
    }

    private CommitStatus createInternalBgpPeerInstance()
        throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createBgpPeerInstance(transaction, HOST, PORT_NUMBER, false, true);
        return transaction.commit();
    }
}
