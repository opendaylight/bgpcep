/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.L2vpnAddressFamily;

public class RIBActivatorTest {
    @Test
    public void testRIBActivator() {
        final RIBActivator ribAct = new RIBActivator();
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        assertNull(context.getRIBSupport(L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class));
        ribAct.startRIBExtensionProvider(context);
        assertNotNull(context.getRIBSupport(L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class));
        ribAct.close();
    }
}