/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

public class SynchronizationTest {

    private final TablesKey ipv4
            = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private final TablesKey linkstate
            = new TablesKey(LinkstateAddressFamily.VALUE, LinkstateSubsequentAddressFamily.VALUE);

    private BGPSynchronization bs;

    private SimpleSessionListener listener;

    private Update ipv4m;

    private Update ipv6m;

    private Update lsm;

    private Update eorm;

    @Before
    public void setUp() {
        listener = new SimpleSessionListener();
        ipv4m = new UpdateBuilder()
                .setNlri(List.of(new NlriBuilder().setPrefix(new Ipv4Prefix("1.1.1.1/32")).build()))
                .build();

        MpReachNlriBuilder mpBuilder = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE);

        AttributesBuilder paBuilder = new AttributesBuilder()
                .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpBuilder.build()).build());

        ipv6m = new UpdateBuilder().setAttributes(paBuilder.build()).build();

        mpBuilder = new MpReachNlriBuilder()
            .setAfi(LinkstateAddressFamily.VALUE)
            .setSafi(LinkstateSubsequentAddressFamily.VALUE);

        paBuilder = new AttributesBuilder()
                .addAugmentation(new AttributesReachBuilder().setMpReachNlri(mpBuilder.build()).build());

        lsm = new UpdateBuilder().setAttributes(paBuilder.build()).build();

        eorm = new UpdateBuilder().build();

        bs = new BGPSynchronization(listener, Set.of(ipv4, linkstate));
    }

    @Test
    public void testSynchronize() {
        // simulate sync
        bs.updReceived(ipv6m);
        bs.updReceived(ipv4m);
        bs.updReceived(lsm);
        bs.kaReceived(); // nothing yet
        assertFalse(bs.syncStorage.get(linkstate).getEor());
        assertFalse(bs.syncStorage.get(ipv4).getEor());
        bs.updReceived(ipv4m);
        bs.kaReceived(); // linkstate
        assertTrue(bs.syncStorage.get(linkstate).getEor());
        bs.kaReceived(); // ipv4 sync
        assertTrue(bs.syncStorage.get(ipv4).getEor());
    }

    @Test
    public void testSynchronizeWithEOR() {
        bs.updReceived(ipv4m);
        bs.updReceived(lsm);
        // Ipv4 Unicast synchronized by EOR message
        assertFalse(bs.syncStorage.get(ipv4).getEor());
        bs.updReceived(eorm);
        assertTrue(bs.syncStorage.get(ipv4).getEor());
        // Linkstate not synchronized yet
        assertFalse(bs.syncStorage.get(linkstate).getEor());
        bs.kaReceived();
        // no message sent by BGPSychchronization
        assertEquals(0, listener.getListMsg().size());
        bs.kaReceived();
        assertTrue(bs.syncStorage.get(linkstate).getEor());
    }
}
