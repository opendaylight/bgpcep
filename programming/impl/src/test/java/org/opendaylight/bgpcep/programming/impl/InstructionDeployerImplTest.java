/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import io.netty.util.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfigKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class InstructionDeployerImplTest extends AbstractConcurrentDataBrokerTest {
    @Mock
    private RpcProviderRegistry rpcRegistry;
    @Mock
    private NotificationPublishService notifs;
    @Mock
    private Timer timer;
    @Mock
    private ClusterSingletonServiceProvider cssp;
    @Mock
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ServiceRegistration serviceRegistration;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doAnswer(invocationOnMock -> InstructionDeployerImplTest.this.singletonServiceRegistration).when(this.cssp)
            .registerClusterSingletonService(any(ClusterSingletonService.class));

        doReturn(this.serviceRegistration).when(this.bundleContext).registerService(any(String.class), any(), any());
    }

    @Test
    public void testInstructionDeployer() throws Exception {
        final InstructionDeployerImpl deployer = new InstructionDeployerImpl(getDataBroker(), this.rpcRegistry,
            this.notifs, this.timer, this.cssp, this.bundleContext);

        checkPresentConfiguration(getDataBroker(), deployer.getInstructionIID());

        final String instructionId = "newInstruction";
        deployer.writeConfiguration(instructionId);
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
        checkNotPresentConfiguration(getDataBroker(), deployer.getInstructionIID());
    }
}