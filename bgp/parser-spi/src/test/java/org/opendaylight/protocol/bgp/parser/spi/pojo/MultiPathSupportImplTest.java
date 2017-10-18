/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class MultiPathSupportImplTest {

    @Test(expected=NullPointerException.class)
    public void testcreateParserMultiPathSupportNull() {
        MultiPathSupportImpl.createParserMultiPathSupport(null);
    }

    @Test
    public void testIsTableTypeSupported() {
        final List<AddressFamilies> supportedTables = Lists.newArrayList();
        final BgpTableType ipv4Unicast = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final BgpTableType ipv4L3vpn = new BgpTableTypeImpl(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class);
        final BgpTableType ipv6Unicast = new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
        final BgpTableType ipv6L3vpn = new BgpTableTypeImpl(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class);
        supportedTables.add(createAddPathCapability(ipv4Unicast, SendReceive.Send));
        supportedTables.add(createAddPathCapability(ipv4L3vpn, SendReceive.Receive));
        supportedTables.add(createAddPathCapability(ipv6Unicast, SendReceive.Both));
        final MultiPathSupport multiPathSupport = MultiPathSupportImpl.createParserMultiPathSupport(supportedTables);

        Assert.assertTrue(multiPathSupport.isTableTypeSupported(ipv4Unicast));
        Assert.assertTrue(multiPathSupport.isTableTypeSupported(ipv6Unicast));
        Assert.assertFalse(multiPathSupport.isTableTypeSupported(ipv4L3vpn));
        Assert.assertFalse(multiPathSupport.isTableTypeSupported(ipv6L3vpn));
    }

    private static AddressFamilies createAddPathCapability(final BgpTableType afisafi, final SendReceive mode) {
        return new AddressFamiliesBuilder().setAfi(afisafi.getAfi()).setSafi(afisafi.getSafi()).setSendReceive(mode).build();
    }

}
