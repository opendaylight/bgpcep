/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborTransportConfig.LocalAddress;

public class BgpNeighborTransportConfigLocalAddressBuilderTest {

    private static final String IPV4_ADDRESS = "127.0.0.1";

    @Test
    public void testIpv4() {
        final LocalAddress localAddress = BgpNeighborTransportConfigLocalAddressBuilder.getDefaultInstance(IPV4_ADDRESS);
        Assert.assertEquals(IPV4_ADDRESS, localAddress.getIpAddress().getIpv4Address().getValue());
    }

    @Test
    public void testString() {
        final LocalAddress localAddress = BgpNeighborTransportConfigLocalAddressBuilder.getDefaultInstance("abcd");
        Assert.assertEquals("abcd", localAddress.getString());
    }

}
