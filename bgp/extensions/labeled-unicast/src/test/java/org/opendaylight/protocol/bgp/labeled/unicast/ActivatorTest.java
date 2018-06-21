/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.LabeledUnicastSubsequentAddressFamily;

public class ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BGPActivator act = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(4));
        act.start(context);
        assertEquals(LabeledUnicastSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(4));
        act.close();
    }
}
