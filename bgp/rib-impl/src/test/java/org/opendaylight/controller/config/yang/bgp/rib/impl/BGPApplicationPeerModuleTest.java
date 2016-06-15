/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;

public class BGPApplicationPeerModuleTest extends AbstractRIBImplModuleTest {

    private static final int EXP_INSTANCES = 8;
    private static final String INSTANCE_NAME = "application-peer-instance";
    private static final String INSTANCE_NAME2 = "application-peer-instance-2";
    private static final String FACTORY_NAME = BGPApplicationPeerModuleFactory.NAME;
    private static final ApplicationRibId APP_RIB_ID = new ApplicationRibId("application-peer-test");
    private static final ApplicationRibId NEW_APP_RIB_ID = new ApplicationRibId("new-application-peer-name");

    private ObjectName dataBroker = null;
    private ObjectName ribModule = null;

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
        assertStatus(status, EXP_INSTANCES, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createApplicationPeerInstance();
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, EXP_INSTANCES);
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
        assertStatus(status, 0, 1, EXP_INSTANCES - 1);
        assertEquals(NEW_APP_RIB_ID, getApplicationRibId());
    }

    @Test
    public void testConflictingPeerAddress() throws Exception {
        createApplicationPeerInstance();
        try {
            createApplicationPeerInstance(INSTANCE_NAME2);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("getInstance() failed for ModuleIdentifier"));
            final Throwable ex = e.getCause();
            assertNotNull(ex);
            assertTrue(ex.getMessage().contains("Peer for IpAddress"));
            assertTrue(ex.getMessage().contains("already present"));
        }
    }

    private CommitStatus createApplicationPeerInstance() throws Exception {
        return createApplicationPeerInstance(INSTANCE_NAME);
    }

    private CommitStatus createApplicationPeerInstance(final String instanceName) throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final ObjectName objName = transaction.createModule(BGPApplicationPeerModuleFactory.NAME, instanceName);
        final BGPApplicationPeerModuleMXBean mxBean = transaction.newMXBeanProxy(objName, BGPApplicationPeerModuleMXBean.class);
        final ObjectName dataBrokerON = lookupDomAsyncDataBroker(transaction);
        mxBean.setDataBroker(dataBrokerON);
        mxBean.setBgpPeerId(new BgpId(BGP_ID));
        mxBean.setApplicationRibId(APP_RIB_ID);
        if (this.dataBroker == null) {
            this.dataBroker = createAsyncDataBrokerInstance(transaction);
        }
        if (this.ribModule == null) {
            this.ribModule = createRIBImplModuleInstance(transaction, this.dataBroker);
        }
        mxBean.setTargetRib(this.ribModule);
        return transaction.commit();
    }

    private ApplicationRibId getApplicationRibId() throws InstanceNotFoundException {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();
        final BGPApplicationPeerModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                BGPApplicationPeerModuleMXBean.class);
        return mxBean.getApplicationRibId();
    }

}
