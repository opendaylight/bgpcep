/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpNextHopType.Enumeration;

public class BgpNextHopTypeBuilderTest {

    private static final String IPV4_ADDRESS = "127.0.0.1";
    private static final String IPV6_ADDRESS = "2001:db8:85a3::8a2e:370:7334";

    @Test
    public void testIpv4() {
        final BgpNextHopType bgpNextHopType = BgpNextHopTypeBuilder.getDefaultInstance(IPV4_ADDRESS);
        Assert.assertEquals(IPV4_ADDRESS, bgpNextHopType.getIpAddress().getIpv4Address().getValue());
    }

    @Test
    public void testIpv6() {
        final BgpNextHopType bgpNextHopType = BgpNextHopTypeBuilder.getDefaultInstance(IPV6_ADDRESS);
        Assert.assertEquals(IPV6_ADDRESS, bgpNextHopType.getIpAddress().getIpv6Address().getValue());
    }

    @Test
    public void testEnumLowerCase() {
        final BgpNextHopType bgpNextHopType = BgpNextHopTypeBuilder.getDefaultInstance("self");
        Assert.assertEquals(Enumeration.SELF, bgpNextHopType.getEnumeration());
    }

    @Test
    public void testEnumFirstUpperCase() {
        final BgpNextHopType bgpNextHopType = BgpNextHopTypeBuilder.getDefaultInstance("Self");
        Assert.assertEquals(Enumeration.SELF, bgpNextHopType.getEnumeration());
    }

    @Test
    public void testEnumUpperCase() {
        final BgpNextHopType bgpNextHopType = BgpNextHopTypeBuilder.getDefaultInstance("SELF");
        Assert.assertEquals(Enumeration.SELF, bgpNextHopType.getEnumeration());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalid() {
        BgpNextHopTypeBuilder.getDefaultInstance("error");
    }

}
