/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public class BGPAppPeerInstanceConfigurationTest {

    private static final String INSTANCE_NAME = "instanceName";
    private static final String RIB_ID = "ribId";
    private static final Ipv4Address BGP_ID = new Ipv4Address("127.0.0.1");

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
        assertEquals(INSTANCE_NAME, config.getInstanceName());
    }

}
