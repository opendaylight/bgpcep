/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import io.netty.util.Timer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfigKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@Ignore("Hangs frequently in autorelease")
public class InstructionDeployerImplTest extends AbstractProgrammingTest {
    @Mock
    private NotificationPublishService notifs;
    @Mock
    private Timer timer;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ServiceRegistration serviceRegistration;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        doReturn(this.serviceRegistration).when(this.bundleContext).registerService(any(String.class), any(), any());
        doNothing().when(this.serviceRegistration).unregister();
    }

    @Test
    public void testInstructionDeployer() throws Exception {
        final InstructionDeployerImpl deployer = new InstructionDeployerImpl(getDataBroker(), this.rpcRegistry,
            this.notifs, this.timer, this.cssp, this.bundleContext);
        checkPresentConfiguration(getDataBroker(), deployer.getInstructionIID());

        final String instructionId = "newInstruction";
        deployer.writeConfiguration(instructionId);
        this.singletonService.instantiateServiceInstance();
        final KeyedInstanceIdentifier<OdlProgrammingConfig, OdlProgrammingConfigKey> intructionIID =
            deployer.getInstructionIID().child(OdlProgrammingConfig.class, new OdlProgrammingConfigKey(instructionId));
        checkPresentConfiguration(getDataBroker(), intructionIID);
        verify(this.cssp, timeout(100)).registerClusterSingletonService(any());
        verify(this.bundleContext, timeout(100)).registerService(any(String.class), any(), any());

        deployer.removeConfiguration(instructionId);
        checkNotPresentConfiguration(getDataBroker(), intructionIID);
        verify(this.singletonServiceRegistration, timeout(100)).close();
        verify(this.serviceRegistration, timeout(100)).unregister();

        deployer.close();
    }
}