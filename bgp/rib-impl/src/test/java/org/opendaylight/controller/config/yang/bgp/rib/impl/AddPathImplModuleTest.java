/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.mockito.Mockito.doReturn;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

public class AddPathImplModuleTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = AddPathImplModuleFactory.NAME;
    private static final String INSTANCE_NAME = "add-path-instance";

    private static final String AFI_NAME = BGPTableTypeImplModuleFactory.NAME;
    private static final String AFI_INSTANCE_NAME = "bgp-table-type-instance";

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext,
            new AddPathImplModuleFactory(),
            new BGPTableTypeImplModuleFactory()));
    }

    @Override
    protected BindingRuntimeContext getBindingRuntimeContext() {
        final BindingRuntimeContext ret = super.getBindingRuntimeContext();
        doReturn(Ipv4AddressFamily.class).when(ret).getIdentityClass(Ipv4AddressFamily.QNAME);
        doReturn(MplsLabeledVpnSubsequentAddressFamily.class).when(ret).getIdentityClass(MplsLabeledVpnSubsequentAddressFamily.QNAME);
        return ret;
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 2, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final AddPathImplModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), AddPathImplModuleMXBean.class);
        mxBean.setSendReceive(SendReceive.Receive);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 1);
    }

    private CommitStatus createInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        createAddpathInstance(transaction);
        return transaction.commit();
    }

    private static ObjectName createAddpathInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final AddPathImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, AddPathImplModuleMXBean.class);

        mxBean.setAddressFamily(createAddressFamily(transaction, AFI_INSTANCE_NAME));
        mxBean.setSendReceive(SendReceive.Both);
        return nameCreated;
    }

    protected static ObjectName createAddressFamily(final ConfigTransactionJMXClient transaction, final String instanceName) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(AFI_NAME, instanceName);
        final BGPTableTypeImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, BGPTableTypeImplModuleMXBean.class);

        final IdentityAttributeRef afiRef = new IdentityAttributeRef(Ipv4AddressFamily.QNAME.toString());
        final IdentityAttributeRef safiRef = new IdentityAttributeRef(MplsLabeledVpnSubsequentAddressFamily.QNAME.toString());

        mxBean.setAfi(afiRef);
        mxBean.setSafi(safiRef);
        return nameCreated;
    }

}
