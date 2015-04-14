/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;

public class BGPApplicationPeerModuleTest extends AbstractRIBImplModuleTest {

    private static final String INSTANCE_NAME = "application-peer-instance";
    private static final String FACTORY_NAME = BGPApplicationPeerModuleFactory.NAME;
    private static final ApplicationRibId APP_RIB_ID = new ApplicationRibId("application-peer-test");
    private static final ApplicationRibId NEW_APP_RIB_ID = new ApplicationRibId("new-application-peer-name");

    @Override
    protected List<ModuleFactory> getModuleFactories() {
        final List<ModuleFactory> moduleFactories = super.getModuleFactories();
        moduleFactories.add(new BGPApplicationPeerModuleFactory());
        return moduleFactories;
    }

    @Test
    public void testCreateInstance() throws Exception {
        final CommitStatus status = createApplicationPeerInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createApplicationPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
    }

    @Test
    public void testReconfigure() throws Exception {
        createApplicationPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final BGPApplicationPeerModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPApplicationPeerModuleMXBean.class);
        mxBean.setApplicationRibId(new ApplicationRibId(NEW_APP_RIB_ID));
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 13);
        assertEquals(NEW_APP_RIB_ID, getApplicationRibId());
    }

    private CommitStatus createApplicationPeerInstance() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName objName = transaction.createModule(BGPApplicationPeerModuleFactory.NAME, INSTANCE_NAME);
        final BGPApplicationPeerModuleMXBean mxBean = transaction.newMXBeanProxy(objName, BGPApplicationPeerModuleMXBean.class);
        final ObjectName dataBrokerON = lookupDomAsyncDataBroker(transaction);
        mxBean.setDataBroker(dataBrokerON);
        mxBean.setBgpPeerId(BGP_ID);
        mxBean.setApplicationRibId(APP_RIB_ID);
        mxBean.setTargetRib(createRIBImplModuleInstance(transaction, createAsyncDataBrokerInstance(transaction)));
        return transaction.commit();
    }

    private ApplicationRibId getApplicationRibId() throws InstanceNotFoundException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final BGPApplicationPeerModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPApplicationPeerModuleMXBean.class);
        return mxBean.getApplicationRibId();
    }

}
