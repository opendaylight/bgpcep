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

import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;

public class RIBImplModuleTest extends AbstractRIBImplModuleTest {
    private static final String INSTANCE_NAME = "rib-impl";
    private static final String FACTORY_NAME = RIBImplModuleFactory.NAME;

    private static final String RIB_ID = "test";
    private static final String BGP_ID = "192.168.1.1";

    @Test
    public void testValidationExceptionRibIdNotSet() throws Exception {
        try {
            createRIBImplModuleInstance(null, 500L, new Ipv4Address(BGP_ID));
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("RibId is not set."));
        }
    }

    @Test
    public void testValidationExceptionLocalAsNotSet() throws Exception {
        try {
            createRIBImplModuleInstance(new RibId(RIB_ID), null, new Ipv4Address(BGP_ID));
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("LocalAs is not set."));
        }
    }

    @Test
    public void testValidationExceptionBgpIdNotSet() throws Exception {
        try {
            createRIBImplModuleInstance(new RibId(RIB_ID), 500L, null);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("BgpId is not set."));
        }
    }

    @Test
    public void testCreateBean() throws Exception {
        CommitStatus status = createInstance();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 14, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createInstance();
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 14);
    }

    @Test
    public void testReconfigure() throws Exception {
        createInstance();
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final RIBImplModuleMXBean mxBean = transaction.newMBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME),
                RIBImplModuleMXBean.class);
        mxBean.setLocalAs(100L);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 1, 13);
    }
}
