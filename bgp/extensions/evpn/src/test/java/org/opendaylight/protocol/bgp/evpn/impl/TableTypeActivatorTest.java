/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.SimpleBGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;

public class TableTypeActivatorTest {

    private static final BgpTableType EVPN = new BgpTableTypeImpl(
            L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class);

    @Test
    public void testActivator() {
        final TableTypeActivator tableTypeActivator = new TableTypeActivator();
        final SimpleBGPTableTypeRegistryProvider registry = new SimpleBGPTableTypeRegistryProvider();
        tableTypeActivator.startBGPTableTypeRegistryProvider(registry);

        final Optional<Class<? extends AfiSafiType>> afiSafiType = registry.getAfiSafiType(EVPN);
        Assert.assertEquals(L2VPNEVPN.class, afiSafiType.get());

        final Optional<BgpTableType> tableType = registry.getTableType(L2VPNEVPN.class);
        Assert.assertEquals(EVPN, tableType.get());

        tableTypeActivator.stopBGPTableTypeRegistryProvider();
        tableTypeActivator.close();
    }

}
