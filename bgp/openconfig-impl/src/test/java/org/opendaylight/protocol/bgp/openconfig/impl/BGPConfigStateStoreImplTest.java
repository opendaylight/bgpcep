/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;

public class BGPConfigStateStoreImplTest {

    private static final BGPConfigStateStoreImpl STORE = new BGPConfigStateStoreImpl();

    @Test
    public void test() {
        assertNull(STORE.getBGPConfigHolder(Neighbor.class));
        STORE.registerBGPConfigHolder(Neighbor.class);
        assertNotNull(STORE.getBGPConfigHolder(Neighbor.class));
    }

}
