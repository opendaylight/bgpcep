/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.bgp.state.StateProviderImplTest.TABLES_KEY;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;

public class GlobalUtilTest {
    @Mock
    private BGPRIBState ribState;
    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(Optional.empty()).when(this.tableRegistry).getAfiSafiType(Mockito.eq(TABLES_KEY));
    }

    @Test
    public void testNonSupportedAfiSafi() {
        assertNull(GlobalUtil.buildAfiSafi(this.ribState, TABLES_KEY, this.tableRegistry));
    }
}