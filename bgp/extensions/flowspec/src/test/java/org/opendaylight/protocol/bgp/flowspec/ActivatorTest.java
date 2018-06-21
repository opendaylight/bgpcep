/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;

public class ActivatorTest {
    private static final int FLOWSPEC_SAFI = 133;
    private static final int FLOWSPEC_VPN_SAFI = 134;

    @Test
    public void testActivator() throws Exception {
        final SimpleFlowspecExtensionProviderContext fsContext = new SimpleFlowspecExtensionProviderContext();
        final FlowspecActivator activator = new FlowspecActivator(fsContext);
        final BGPActivator act = new BGPActivator(activator);
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(FLOWSPEC_SAFI));
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(FLOWSPEC_VPN_SAFI));
        act.start(context);
        assertEquals(FlowspecSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(FLOWSPEC_SAFI));
        assertEquals(FlowspecL3vpnSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(FLOWSPEC_VPN_SAFI));
        act.close();
    }
}

