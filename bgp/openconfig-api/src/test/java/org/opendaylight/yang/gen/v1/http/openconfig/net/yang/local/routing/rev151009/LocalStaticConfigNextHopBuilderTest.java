/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev151009;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev151009.LocalStaticConfig.NextHop;

public class LocalStaticConfigNextHopBuilderTest {

    private static final String IPV4_ADDRESS = "127.0.0.1";
    private static final String STRING = "host";

    @Test
    public void testIpAddress() {
        final NextHop nextHop = LocalStaticConfigNextHopBuilder.getDefaultInstance(IPV4_ADDRESS);
        Assert.assertEquals(IPV4_ADDRESS, nextHop.getIpAddress().getIpv4Address().getValue());
    }

    @Test
    public void testEnum() {
        final NextHop nextHop = LocalStaticConfigNextHopBuilder.getDefaultInstance(LocalDefinedNextHop.DROP.toString());
        Assert.assertEquals(LocalDefinedNextHop.DROP, nextHop.getLocalDefinedNextHop());
    }

    @Test
    public void testString() {
        final NextHop nextHop = LocalStaticConfigNextHopBuilder.getDefaultInstance(STRING);
        Assert.assertEquals(STRING, nextHop.getString());
    }

}
