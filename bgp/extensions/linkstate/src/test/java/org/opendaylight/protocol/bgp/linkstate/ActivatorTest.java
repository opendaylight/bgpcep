/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator;
import org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.ServiceLoaderRSVPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;

public class ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BGPActivator act = new BGPActivator(true, ServiceLoaderRSVPExtensionProviderContext.getSingletonInstance().getRsvpRegistry());
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertNull(context.getAddressFamilyRegistry().classForFamily(16388));
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(71));

        act.start(context);

        assertEquals(LinkstateAddressFamily.class, context.getAddressFamilyRegistry().classForFamily(16388));
        assertEquals(LinkstateSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(71));

        act.close();
    }

    @Test
    public void testRIBActivator() {
        final RIBActivator ribAct = new RIBActivator();
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();

        assertNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.startRIBExtensionProvider(context);

        assertNotNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.close();
    }
}
