/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBActivatorTest;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;

public class RIBActivatorTest extends AbstractRIBActivatorTest {
    @Test
    public void testRIBActivator() {
        final RIBActivator ribAct = new RIBActivator();
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        assertNull(context.getRIBSupport(Ipv4AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class));
        assertNull(context.getRIBSupport(Ipv6AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class));
        ribAct.startRIBExtensionProvider(context, this.mappingService);
        assertNotNull(context.getRIBSupport(Ipv4AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class));
        assertNotNull(context.getRIBSupport(Ipv6AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class));
        ribAct.close();
    }
}