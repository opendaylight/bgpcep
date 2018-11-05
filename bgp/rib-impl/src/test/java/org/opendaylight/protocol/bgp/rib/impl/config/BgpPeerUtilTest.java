/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;

public class BgpPeerUtilTest {

    private final TablesKey IPV4_TABLE_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private final TablesKey IPV6_TABLE_KEY = new TablesKey(Ipv6AddressFamily.class,
            UnicastSubsequentAddressFamily.class);

    @Test
    public void createIpv4EORTest() {
        final Update endOfRib = BgpPeerUtil.createEndOfRib(IPV4_TABLE_KEY);
        assertNull(endOfRib.getNlri());
        assertNull(endOfRib.getWithdrawnRoutes());
        assertNull(endOfRib.getAttributes());
    }

    @Test
    public void createNonIpv4EORTest() {
        final Update endOfRib = BgpPeerUtil.createEndOfRib(IPV6_TABLE_KEY);
        assertNull(endOfRib.getNlri());
        assertNull(endOfRib.getWithdrawnRoutes());
        final Attributes attributes = endOfRib.getAttributes();
        assertNotNull(attributes);
        final Attributes2 augmentation = attributes.augmentation(Attributes2.class);
        assertNotNull(augmentation);
        final MpUnreachNlri mpUnreachNlri = augmentation.getMpUnreachNlri();
        assertNotNull(mpUnreachNlri);
        assertEquals(IPV6_TABLE_KEY.getAfi(), mpUnreachNlri.getAfi());
        assertEquals(IPV6_TABLE_KEY.getSafi(), mpUnreachNlri.getSafi());
        assertNull(mpUnreachNlri.getWithdrawnRoutes());
    }
}
