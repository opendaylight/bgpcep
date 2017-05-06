/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;

public class DemoBugTest extends AbstractConcurrentDataBrokerTest {
    @Test
    public void testInstructionDeployer() throws Exception {
        final DemoClass deployer = new DemoClass(getDataBroker());
        checkPresentConfiguration(getDataBroker(), deployer.getInstructionIID());
        deployer.close();
    }
}
