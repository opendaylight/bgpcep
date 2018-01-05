/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.protocol.bgp.rib.spi.util.ClusterSingletonServiceRegistrationHelper.registerSingletonService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;

public class ClusterSingletonServiceRegistrationHelperTest {

    @Mock
    private ClusterSingletonService clusterSingletonService;
    @Mock
    private ClusterSingletonServiceProvider singletonProvider;
    @Mock
    private ClusterSingletonServiceRegistration serviceRegistration;

    @Before
    public void setUp() {
        initMocks(this);
        doReturn(null).when(this.singletonProvider).registerClusterSingletonService(any());
        Mockito.when(this.singletonProvider.registerClusterSingletonService(any()))
                .thenThrow(new RuntimeException())
                .thenReturn(this.serviceRegistration);
    }

    @Test(expected = RuntimeException.class)
    public void testRegisterSingletonServiceFailure() {
        registerSingletonService(this.singletonProvider, this.clusterSingletonService, 0, 1);
    }

    @Test
    public void testRegisterSingletonServiceSuccessfulRetry() {
        final ClusterSingletonServiceRegistration registerSingletonService =
                registerSingletonService(this.singletonProvider, this.clusterSingletonService, 1, 1);
        assertEquals(this.serviceRegistration, registerSingletonService);
        //first reg. attempt failed, second succeeded
        verify(this.singletonProvider, Mockito.times(2)).registerClusterSingletonService(any());
    }

}
