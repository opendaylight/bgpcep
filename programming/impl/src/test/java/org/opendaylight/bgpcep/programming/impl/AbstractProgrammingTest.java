/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.ProgrammingService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

abstract class AbstractProgrammingTest extends AbstractConcurrentDataBrokerTest {
    @Mock
    RpcProviderService rpcRegistry;
    @Mock
    ClusterSingletonServiceProvider cssp;
    @Mock
    ClusterSingletonServiceRegistration singletonServiceRegistration;
    @Mock
    private ObjectRegistration<ProgrammingService> registration;
    ClusterSingletonService singletonService;

    AbstractProgrammingTest() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doAnswer(invocationOnMock -> {
            this.singletonService = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            return this.singletonServiceRegistration;
        }).when(this.cssp).registerClusterSingletonService(any(ClusterSingletonService.class));
        doAnswer(invocationOnMock -> {
            this.singletonService.closeServiceInstance().get();
            return null;
        }).when(this.singletonServiceRegistration).close();

        doReturn(this.registration).when(this.rpcRegistry).registerRpcImplementation(any(),
            any(ProgrammingService.class));

        doNothing().when(this.registration).close();
    }
}
