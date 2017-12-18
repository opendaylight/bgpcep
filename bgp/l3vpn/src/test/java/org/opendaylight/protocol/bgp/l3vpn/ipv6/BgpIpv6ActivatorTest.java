/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;

public class BgpIpv6ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BgpIpv6Activator act = new BgpIpv6Activator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertFalse(context.getNlriRegistry().getSerializers().iterator().hasNext());
        act.start(context);
        assertTrue(context.getNlriRegistry().getSerializers().iterator().next() instanceof VpnIpv6NlriParser);
        act.close();
    }
}