/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleBGPTableTypeRegistryProviderActivatorTest {
    @Mock
    private BGPTableTypeRegistryProviderActivator providerActivator;

    @Test
    public void testSimpleBGPTableTypeRegistryProviderActivator() {
        doReturn(List.of()).when(providerActivator)
            .startBGPTableTypeRegistryProvider(any(BGPTableTypeRegistryProvider.class));

        final DefaultBGPTableTypeRegistryProvider provider = new DefaultBGPTableTypeRegistryProvider(
            List.of(providerActivator));
        verify(providerActivator).startBGPTableTypeRegistryProvider(provider);
    }
}
