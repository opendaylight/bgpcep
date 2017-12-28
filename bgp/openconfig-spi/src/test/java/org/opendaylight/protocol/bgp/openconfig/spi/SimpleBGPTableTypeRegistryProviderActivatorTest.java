/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SimpleBGPTableTypeRegistryProviderActivatorTest {

    @Mock
    private BGPTableTypeRegistryProviderActivator providerActivator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSimpleBGPTableTypeRegistryProviderActivator() {
        final SimpleBGPTableTypeRegistryProvider provider = new SimpleBGPTableTypeRegistryProvider();
        final SimpleBGPTableTypeRegistryProviderActivator activator =
                new SimpleBGPTableTypeRegistryProviderActivator(provider,
                        Collections.singletonList(this.providerActivator));
        activator.start();
        verify(this.providerActivator).startBGPTableTypeRegistryProvider(Mockito.any());
        activator.close();
        verify(this.providerActivator).stopBGPTableTypeRegistryProvider();
    }

}
