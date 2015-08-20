/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.linkstate;

import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.bgp.parser.spi.SimpleBGPExtensionProviderContextModuleTest;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.spi.RIBExtensionsImplModuleTest;
import org.opendaylight.controller.config.yang.rsvp.spi.SimpleRSVPExtensionProviderContextModuleFactory;
import org.opendaylight.controller.config.yang.rsvp.spi.SimpleRSVPExtensionProviderContextModuleMXBean;

public class LinkstateModuleTest extends AbstractConfigTest {

    private static final String FACTORY_NAME = LinkstateModuleFactory.NAME;
    private static final String INSTANCE_NAME = "bgp-linkstate-impl";
    private static final String RSVP_EXTENSION_INSTANCE_NAME = "extension-impl-rsvp";
    private ObjectName rspvInstance;

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, new
            LinkstateModuleFactory(), new SimpleBGPExtensionProviderContextModuleFactory(), new
            RIBExtensionsImplModuleFactory(), new SimpleRSVPExtensionProviderContextModuleFactory()));
    }

    @Test
    public void testLSTypeDefaultValue() throws Exception {
        final CommitStatus status = createLinkstateModuleInstance(Optional.<Boolean>absent());
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 4, 0, 0);
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        final LinkstateModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), LinkstateModuleMXBean.class);
        assertTrue(mxBean.getIanaLinkstateAttributeType());
    }

    @Test
    public void testCreateBean() throws Exception {
        final CommitStatus status = createLinkstateModuleInstance(Optional.of(false));
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 4, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws Exception {
        createLinkstateModuleInstance(Optional.of(false));
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 0, 4);
    }

    @Test
    public void testReconfigureInstance() throws Exception {
        createLinkstateModuleInstance(Optional.of(false));
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, FACTORY_NAME);
        final LinkstateModuleMXBean mxBean = transaction.newMXBeanProxy(transaction.lookupConfigBean(FACTORY_NAME, INSTANCE_NAME), LinkstateModuleMXBean.class);
        mxBean.setIanaLinkstateAttributeType(true);
        mxBean.setRsvpExtensions(rspvInstance);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, FACTORY_NAME);
        assertStatus(status, 0, 3, 1);
    }

    private CommitStatus createLinkstateModuleInstance(final Optional<Boolean> ianaLSAttributeType) throws Exception {
        final ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        final ObjectName linkstateON = transaction.createModule(FACTORY_NAME, INSTANCE_NAME);
        final LinkstateModuleMXBean mxBean = transaction.newMXBeanProxy(linkstateON, LinkstateModuleMXBean.class);
        if(ianaLSAttributeType.isPresent()) {
            mxBean.setIanaLinkstateAttributeType(ianaLSAttributeType.get());
        }
        rspvInstance = createRsvpExtensionsInstance(transaction);
        mxBean.setRsvpExtensions(rspvInstance);
        SimpleBGPExtensionProviderContextModuleTest.createBGPExtensionsModuleInstance(transaction, Lists.newArrayList(linkstateON));
        RIBExtensionsImplModuleTest.createRIBExtensionsModuleInstance(transaction, Lists.newArrayList(linkstateON));
        return transaction.commit();
    }

    private ObjectName createRsvpExtensionsInstance(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(SimpleRSVPExtensionProviderContextModuleFactory.NAME,
            RSVP_EXTENSION_INSTANCE_NAME);
        transaction.newMXBeanProxy(nameCreated, SimpleRSVPExtensionProviderContextModuleMXBean.class);
        return nameCreated;
    }
}
