/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
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

    @Test
    public void isEndOfTableTest() {
        final Update ipv4EOT = new UpdateBuilder().build();
        final MpUnreachNlri ipv6EOTnlri = new MpUnreachNlriBuilder()
                .setAfi(IPV6_TABLE_KEY.getAfi())
                .setSafi(IPV6_TABLE_KEY.getSafi())
                .build();
        final Update ipv6EOT = new UpdateBuilder()
                .setAttributes(new AttributesBuilder()
                        .addAugmentation(Attributes2.class, new Attributes2Builder()
                                .setMpUnreachNlri(ipv6EOTnlri)
                                .build())
                        .build())
                .build();

        assertTrue(BgpPeerUtil.isEndOfRib(ipv4EOT));
        assertTrue(BgpPeerUtil.isEndOfRib(ipv6EOT));

        final Update ipv4NonEOT = new UpdateBuilder()
                .setNlri(Collections.emptyList())
                .build();
        final MpUnreachNlri ipv6NonEOTnlri = new MpUnreachNlriBuilder(ipv6EOTnlri)
                .setWithdrawnRoutes(new WithdrawnRoutesBuilder().build())
                .build();
        final Update ipv6NonEOT = new UpdateBuilder()
                .setAttributes(new AttributesBuilder()
                        .addAugmentation(Attributes2.class, new Attributes2Builder()
                                .setMpUnreachNlri(ipv6NonEOTnlri)
                                .build())
                        .build())
                .build();

        assertFalse(BgpPeerUtil.isEndOfRib(ipv4NonEOT));
        assertFalse(BgpPeerUtil.isEndOfRib(ipv6NonEOT));
    }
}
