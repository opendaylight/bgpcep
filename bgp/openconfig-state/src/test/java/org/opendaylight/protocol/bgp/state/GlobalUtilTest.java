/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.state.StateProviderImplTest.TABLES_KEY;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;

@ExtendWith(MockitoExtension.class)
class GlobalUtilTest {
    @Mock
    private BGPRibState ribState;
    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;

    @Test
    void testNonSupportedAfiSafi() {
        doReturn(null).when(tableRegistry).getAfiSafiType(eq(TABLES_KEY));
        assertNull(GlobalUtil.buildAfiSafi(ribState, TABLES_KEY, tableRegistry));
    }
}
