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
