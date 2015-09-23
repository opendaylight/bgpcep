/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPPeerInstanceConfiguration;

public class BGPConfigMapperRegistryTest {

    @SuppressWarnings("rawtypes")
    final AbstractBGPOpenConfigMapper mapper = Mockito.mock(AbstractBGPOpenConfigMapper.class);

    @Before
    public void setup() {
        Mockito.doReturn(BGPPeerInstanceConfiguration.class).when(mapper).getInstanceConfigurationType();
    }

    @Test
    public void testBGPConfigMapperRegistry() {
        final BGPConfigMapperRegistry registry = new BGPConfigMapperRegistry();
        registry.registerOpenConfigMapper(mapper);
        assertNotNull(registry.getOpenConfigMapper(BGPPeerInstanceConfiguration.class));
        assertNull(registry.getOpenConfigMapper(BGPAppPeerInstanceConfiguration.class));
        assertNull(registry.getOpenConfigMapper(InstanceConfiguration.class));
    }

}
