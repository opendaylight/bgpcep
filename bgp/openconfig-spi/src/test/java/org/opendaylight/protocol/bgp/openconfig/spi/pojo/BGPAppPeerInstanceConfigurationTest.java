/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;

import static org.junit.Assert.assertEquals;

public class BGPAppPeerInstanceConfigurationTest {

    private static final InstanceConfigurationIdentifier INSTANCE_NAME = new InstanceConfigurationIdentifier("instanceName");
    private static final String RIB_ID = "ribId";
    private static final BgpId BGP_ID = new BgpId("127.0.0.1");

    private final BGPAppPeerInstanceConfiguration config = new BGPAppPeerInstanceConfiguration(INSTANCE_NAME, RIB_ID, BGP_ID);

    @Test
    public void testGetAppRibId() {
        assertEquals(RIB_ID, config.getAppRibId());
    }

    @Test
    public void testGetBgpId() {
        assertEquals(BGP_ID, config.getBgpId());
    }

    @Test
    public void testGetInstanceName() {
        assertEquals(INSTANCE_NAME, config.getIdentifier());
    }

}
