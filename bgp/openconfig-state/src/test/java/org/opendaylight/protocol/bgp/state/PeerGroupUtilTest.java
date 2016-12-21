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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;

public class PeerGroupUtilTest {
    @Mock
    private BGPPeerState bgpPeerState;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(this.bgpPeerState).getGroupId();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPeerGroupUtilPrivateConstructor() throws Throwable {
        final Constructor<PeerGroupUtil> c = PeerGroupUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testNoneGroup() {
        assertNull(PeerGroupUtil.buildPeerGroups(Collections.singletonList(this.bgpPeerState)));
    }
}