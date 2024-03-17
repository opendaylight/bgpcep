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

import org.junit.Before;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;

abstract class AbstractProgrammingTest extends AbstractConcurrentDataBrokerTest {
    @Mock
    RpcProviderService rpcRegistry;
    @Mock
    ClusterSingletonServiceProvider cssp;
    @Mock
    Registration singletonServiceRegistration;
    @Mock
    private Registration registration;
    ClusterSingletonService singletonService;

    AbstractProgrammingTest() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        doAnswer(invocationOnMock -> {
            singletonService = invocationOnMock.getArgument(0);
            return singletonServiceRegistration;
        }).when(cssp).registerClusterSingletonService(any(ClusterSingletonService.class));
        doAnswer(invocationOnMock -> {
            singletonService.closeServiceInstance().get();
            return null;
        }).when(singletonServiceRegistration).close();

        doReturn(registration).when(rpcRegistry).registerRpcImplementations(any(Rpc[].class));

        doNothing().when(registration).close();
    }
}
