/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;

public class TableTypeActivatorTest {
    private static final BgpTableType EVPN = new BgpTableTypeImpl(
            L2vpnAddressFamily.VALUE, EvpnSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(L2VPNEVPN.VALUE, registry.getAfiSafiType(EVPN));
        assertEquals(EVPN, registry.getTableType(L2VPNEVPN.VALUE));
    }
}
