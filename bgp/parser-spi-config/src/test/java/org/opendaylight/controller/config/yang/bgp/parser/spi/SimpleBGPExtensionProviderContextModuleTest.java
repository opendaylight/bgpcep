/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.parser.spi;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class SimpleBGPExtensionProviderContextModuleTest extends AbstractConfigTest {

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(new SimpleBGPExtensionProviderContextModuleFactory()));
    }


    @Test
    public void testReuse() throws Exception {
        ConfigTransactionJMXClient tx1 = configRegistryClient.createTransaction();
        tx1.createModule(SimpleBGPExtensionProviderContextModuleFactory.NAME, "instanceName");
        CommitStatus status1 = tx1.commit();
        assertStatus(status1, 1, 0, 0);
        ConfigTransactionJMXClient tx2 = configRegistryClient.createTransaction();
        CommitStatus status2 = tx2.commit();
        assertStatus(status2, 0, 0, 1);
    }


}
