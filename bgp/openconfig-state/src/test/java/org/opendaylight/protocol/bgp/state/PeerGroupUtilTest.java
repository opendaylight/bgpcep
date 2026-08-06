/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;

@ExtendWith(MockitoExtension.class)
class PeerGroupUtilTest {
    @Mock
    private BGPPeerState bgpPeerState;

    @BeforeEach
    void setUp() {
        doReturn(null).when(bgpPeerState).getGroupId();
    }

    @Test
    void testNoneGroup() {
        assertNull(PeerGroupUtil.buildPeerGroups(List.of(bgpPeerState)));
    }
}
