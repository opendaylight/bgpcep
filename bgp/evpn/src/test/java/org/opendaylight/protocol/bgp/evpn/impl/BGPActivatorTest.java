/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.L2vpnAddressFamily;

public class BGPActivatorTest {
    @Test
    public void testActivator() throws Exception {
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertNull(context.getAddressFamilyRegistry().classForFamily(25));
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(70));
        act.start(context);
        assertEquals(L2vpnAddressFamily.class, context.getAddressFamilyRegistry().classForFamily(25));
        assertEquals(EvpnSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(70));
        act.close();
    }
}