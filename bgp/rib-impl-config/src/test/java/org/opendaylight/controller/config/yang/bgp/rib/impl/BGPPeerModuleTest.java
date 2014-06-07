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
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;

public class BGPPeerModuleTest extends AbstractRIBImplModuleTest {

    private static final String INSTANCE_NAME = "bgp-peer-module-impl";
    private static final String FACTORY_NAME = BGPPeerModuleFactory.NAME;

    private static final String HOST = "127.0.0.1";
    private static final PortNumber portNumber = new PortNumber(1);

    @Override
    protected CodecRegistry getCodecRegistry() {
        IdentityCodec<?> idCodec = mock(IdentityCodec.class);
        doReturn(Ipv4AddressFamily.class).when(idCodec).deserialize(Ipv4AddressFamily.QNAME);
        doReturn(MplsLabeledVpnSubsequentAddressFamily.class).when(idCodec).deserialize(MplsLabeledVpnSubsequentAddressFamily.QNAME);

        CodecRegistry codecReg = super.getCodecRegistry();
        doReturn(idCodec).when(codecReg).getIdentityCodec();
        return codecReg;
    }

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new BGPPeerModuleFactory());
        moduleFactories.add(new BGPTableTypeImplModuleFactory());
        return moduleFactories;
    }

    @Test
    public void testValidationExceptionPortNotSet() throws Exception {
        try {
            createBgpPeerInstance(HOST, null);
            fail();
        } catch (final ValidationException e) {
            assertTrue(e.getMessage().contains("Port value is not set."));
        }
    }

    @Test
    public void testValidationExceptionHostNotSet() throws Exception {
        try {
            createBgpPeerInstance(null, portNumber);
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
    public void testReusingOldInstance() throws Exception {
        CommitStatus status = createBgpPeerInstance();
        ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 16);
    }

    @Test
    public void testReconfigure() throws Exception {
        CommitStatus status = createBgpPeerInstance();
        ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPPeerModuleMXBean.class);
        mxBean.setPort(new PortNumber(10));
        status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 15);
    }

    private ObjectName createBgpPeerInstance(final ConfigTransactionJMXClient transaction, final String host, final PortNumber port)
            throws Exception {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final BGPPeerModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPPeerModuleMXBean.class);

        // FIXME JMX crashes if union was not created via artificial constructor
        // annotated for JMX as value
        // IpAddress host1 = new IpAddress(new Ipv4Address(host));
        mxBean.setHost(host == null ? null : new IpAddress(host.toCharArray()));
        mxBean.setPort(port);
        mxBean.setAdvertizedTable(Collections.<ObjectName> emptyList());
        mxBean.setRib(createRIBImplModuleInstance(transaction));
        mxBean.setAdvertizedTable(Lists.newArrayList(BGPTableTypeImplModuleTest.createTableInstance(transaction,
                new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString()),
                new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString()))));
        return nameCreated;
    }

    private CommitStatus createBgpPeerInstance() throws Exception {
        return createBgpPeerInstance(HOST, portNumber);
    }

    private CommitStatus createBgpPeerInstance(final String host, final PortNumber port) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createBgpPeerInstance(transaction, host, port);
        return transaction.commit();
    }
}
